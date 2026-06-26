package com.parrotalk.backend.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.parrotalk.backend.config.AppSetting;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.MediaType;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.SourceType;
import com.parrotalk.backend.constant.TranslationStatus;
import com.parrotalk.backend.dto.AdminCreateLessonRequest;
import com.parrotalk.backend.dto.UpdateLessonInfoRequest;
import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.dto.LessonSearchRequest;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.TranscriptionResponse;
import com.parrotalk.backend.dto.SegmentTranslationResponse;
import com.parrotalk.backend.dto.TranslationSummaryResponse;
import com.parrotalk.backend.dto.UpsertSegmentRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.mapper.CategoryMapper;
import com.parrotalk.backend.mapper.LessonMapper;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.repository.SegmentTranslationRepository;
import com.parrotalk.backend.util.YoutubeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lesson Service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    /** Lesson repository */
    private final LessonRepository lessonRepository;

    /** SSE service */
    private final SseService sseService;

    private final LessonMapper lessonMapper;
    private final CategoryMapper categoryMapper;
    private final TranscriptionSegmentRepository transcriptionSegmentRepository;
    private final SegmentTranslationRepository segmentTranslationRepository;
    private final CategoryRepository categoryRepository;
    private final TranslationService translationService;

    /** App setting */
    private final AppSetting appSetting;

    /**
     * Create a new lesson
     * 
     * @param fileUrl  Audio file url
     * @param fileHash Audio file hash
     * @param owner    Owner of the lesson
     * @param title    Lesson title
     * @return Lesson
     */
    @Transactional
    public Lesson createLesson(String fileUrl, String fileHash, User owner, String title) {
        boolean isYoutubeUrl = YoutubeUtil.isYoutubeUrl(fileUrl);
        String thumbnailUrl = isYoutubeUrl
                ? YoutubeUtil.getThumbnailUrl(fileUrl)
                : appSetting.getDefaultAudioThumbnail();
        UUID ownerId = owner.getRole() == Role.ADMIN ? null : owner.getId();

        Lesson lesson = Lesson.builder()
                .fileUrl(fileUrl)
                .fileHash(fileHash)
                .ownerId(ownerId)
                .duration(0)
                .title(title)
                .mediaType(isYoutubeUrl ? MediaType.VIDEO : MediaType.AUDIO)
                .sourceType(isYoutubeUrl ? SourceType.YOUTUBE : SourceType.CLOUDINARY)
                .thumbnailUrl(thumbnailUrl)
                .build();
        return lessonRepository.save(lesson);
    }

    /**
     * Find lesson by file hash.
     * 
     * @param fileHash File hash
     * @return Lesson
     */
    public Optional<Lesson> findReusableLesson(String fileHash, User owner) {
        if (owner.getRole() == Role.ADMIN) {
            return lessonRepository.findByFileHashAndOwnerIdIsNull(fileHash);
        }
        return lessonRepository.findByFileHashAndOwnerId(fileHash, owner.getId());
    }

    public LessonResponse getLessonStatus(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        return lessonMapper.toLessonResponse(lesson);
    }

    /**
     * Get the lesson response.
     * Used by user get lesson status after upload.
     * 
     * @param lessonId Lesson id
     * @return Lesson response
     */
    @Cacheable(value = "lessonDetailCache", key = "#lessonId")
    public LessonResponse getLessonDetail(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        List<TranscriptionResponse> segments = buildSegmentResponses(lesson.getSegments());

        LessonResponse lessonResponse = lessonMapper.toLessonResponse(lesson);
        lessonResponse.setSegments(segments);
        lessonResponse.setTranslationSummary(buildTranslationSummary(lesson.getId(), lesson.getSegments().size()));

        return lessonResponse;
    }

    public List<LessonResponse> getAllLessons(User user) {
        List<Lesson> lessons;
        if (user.getRole() == Role.ADMIN) {
            lessons = lessonRepository.findAll();
        } else {
            // Logic for regular users: see their own lessons
            lessons = lessonRepository.findAllByOwnerId(user.getId());
        }

        return lessons.stream()
                .map(lessonMapper::toLessonResponse)
                .sorted((j1, j2) -> j2.getCreatedAt().compareTo(j1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Search lessons.
     * 
     * @param request Lesson search request
     * @return Page of lessons
     */
    // @Cacheable(value = "lessonSearchCache", key = "#request.getCacheKey()")
    public PageResponse<LessonWithProgressDTO> searchLessons(LessonSearchRequest request, User user) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<LessonWithProgressDTO> lessonPage = lessonRepository.searchPublishedLessonsWithProgress(
                request.getQuery(),
                user.getId(),
                pageable);

        return PageResponse.<LessonWithProgressDTO>builder()
                .content(lessonPage.getContent())
                .page(lessonPage.getNumber())
                .size(lessonPage.getSize())
                .totalElements(lessonPage.getTotalElements())
                .totalPages(lessonPage.getTotalPages())
                .build();
    }

    /**
     * Search own lessons.
     * 
     * @param request Lesson search request
     * @param user    User
     * @return Page of lessons
     */
    public PageResponse<LessonWithProgressDTO> searchMyLessons(LessonSearchRequest request, User user) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<LessonWithProgressDTO> lessonPage = lessonRepository.searchOwnedHiddenLessonsWithProgress(
                request.getQuery(),
                user.getId(),
                pageable);

        return PageResponse.<LessonWithProgressDTO>builder()
                .content(lessonPage.getContent())
                .page(lessonPage.getNumber())
                .size(lessonPage.getSize())
                .totalElements(lessonPage.getTotalElements())
                .totalPages(lessonPage.getTotalPages())
                .build();
    }

    /**
     * Update lesson progress when received from RabbitMQ.
     * 
     * @param lessonId      Lesson ID
     * @param progress      Progress percentage
     * @param step          Current step
     * @param status        Lesson status
     * @param totalSegments Total segments
     */
    @Transactional
    public void updateProgress(Lesson lesson, int progress, String step, LessonStatus status, int totalSegments) {
        lesson.setStatus(status);
        if (LessonStatus.DONE.equals(status) && lesson.getOwnerId() == null) {
            lesson.setVisibilityStatus(LessonVisibilityStatus.PUBLISHED);
        }
        lesson.setProgress(progress);
        lesson.setCurrentStep(step);
        lesson.setTotalSegments(totalSegments);

        lessonRepository.save(lesson);

        sseService.sendUpdatedLessonSSE(lesson);
    }

    @Transactional
    public void deleteLesson(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new RuntimeException("Lesson not found"));
        segmentTranslationRepository.deleteBySegmentLessonId(lessonId);
        transcriptionSegmentRepository.deleteByLessonId(lessonId);
        lessonRepository.delete(lesson);
    }

    /**
     * Get list lessons for Admin.
     * 
     * @param query  Search query
     * @param status Lesson status
     * @param page   Page number
     * @param size   Page size
     * @return Page of lessons
     */
    public PageResponse<LessonResponse> searchAdminLessons(
            String query,
            LessonVisibilityStatus status,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Lesson> lessonPage;
        Specification<Lesson> specification = Specification.where((root, q, cb) -> cb.conjunction());
        if (status != null) {
            specification = specification.and((root, q, cb) -> cb.equal(root.get("visibilityStatus"), status));
        }
        if (StringUtils.isNotBlank(query)) {
            specification = specification
                    .and((root, q, cb) -> cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%"));
        }
        lessonPage = lessonRepository.findAll(specification, pageable);

        List<LessonResponse> content = lessonPage.getContent().stream()
                .map(lessonMapper::toLessonResponse)
                .collect(Collectors.toList());

        return PageResponse.<LessonResponse>builder()
                .content(content)
                .page(lessonPage.getNumber())
                .size(lessonPage.getSize())
                .totalElements(lessonPage.getTotalElements())
                .totalPages(lessonPage.getTotalPages())
                .build();
    }

    @Transactional
    public LessonResponse createAdminLesson(AdminCreateLessonRequest request) {
        Lesson lesson = Lesson.builder()
                .title(request.getTitle().trim())
                .fileUrl(request.getSource().trim())
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .build();

        return lessonMapper.toLessonResponse(lessonRepository.save(lesson));
    }

    /**
     * Update general information of lesson.
     * 
     * @param lessonId Lesson id
     * @param request  Update lesson request
     * @return Updated lesson response
     */
    @Transactional
    public LessonResponse updateGeneralInformation(UUID lessonId, UpdateLessonInfoRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));

        lesson.setTitle(request.getTitle().trim());
        lesson.setFileUrl(request.getSource().trim());
        if (request.getStatus() != null) {
            lesson.setVisibilityStatus(request.getStatus());
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = request.getCategoryIds().stream()
                    .filter(Objects::nonNull)
                    .map(id -> categoryRepository.findById(UUID.fromString(id))
                            .orElseThrow(() -> new ParroTalkException("Category not found",
                                    HttpStatusCode.valueOf(404))))
                    .collect(Collectors.toCollection(HashSet::new));
            lesson.setCategories(categories);
        }
        lesson = lessonRepository.save(lesson);

        LessonResponse response = lessonMapper.toLessonResponse(lesson);
        Set<CategoryResponse> categories = lesson.getCategories().stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toSet());
        response.setCategories(categories);

        return response;
    }

    public LessonResponse getAdminLessonDetail(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));

        List<TranscriptionSegment> lessonSegments = transcriptionSegmentRepository
                .findByLessonIdOrderByDisplayOrderAsc(lessonId);
        List<TranscriptionResponse> segments = buildSegmentResponses(lessonSegments);

        LessonResponse response = lessonMapper.toLessonResponse(lesson);
        response.setSegments(segments);
        response.setTranslationSummary(buildTranslationSummary(lessonId, lessonSegments.size()));
        response.setCategories(lesson.getCategories().stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toSet()));
        return response;
    }

    /**
     * Update lesson visibility.
     * 
     * @param lessonId Lesson ID.
     * @param status   Lesson visibility status.
     * @return Lesson response.
     */
    @Transactional
    public LessonResponse updateLessonVisibility(UUID lessonId, LessonVisibilityStatus status) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));
        lesson.setVisibilityStatus(status);
        return lessonMapper.toLessonResponse(lessonRepository.save(lesson));
    }

    /**
     * Update lesson segments.
     * 
     * @param lessonId          Lesson id.
     * @param requests          List of upsert segment requests.
     * @param deletedSegmentIds List of deleted segment ids.
     * @return Lesson response.
     */
    @Transactional
    @CacheEvict(value = "lessonDetailCache", key = "#lessonId")
    public LessonResponse updateLessonSegments(
            UUID lessonId,
            List<UpsertSegmentRequest> requests,
            List<UUID> deletedSegmentIds) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));

        validateSegments(requests);

        List<TranscriptionSegment> existingSegments = transcriptionSegmentRepository.findByLessonId(lessonId);
        Map<UUID, TranscriptionSegment> existingById = existingSegments.stream()
                .collect(Collectors.toMap(TranscriptionSegment::getId, s -> s));

        if (deletedSegmentIds != null && !deletedSegmentIds.isEmpty()) {
            List<TranscriptionSegment> toDelete = existingSegments.stream()
                    .filter(segment -> deletedSegmentIds.contains(segment.getId()))
                    .collect(Collectors.toList());
            if (!toDelete.isEmpty()) {
                segmentTranslationRepository.deleteBySegmentIdInAndTargetLanguage(
                        toDelete.stream().map(TranscriptionSegment::getId).toList(),
                        TranslationService.DEFAULT_TARGET_LANGUAGE);
                transcriptionSegmentRepository.deleteAll(toDelete);
            }
        }

        Set<UUID> changedTextSegmentIds = new HashSet<>();
        AtomicBoolean hasNewSegments = new AtomicBoolean(false);
        List<TranscriptionSegment> upserts = requests.stream().map(request -> {
            TranscriptionSegment segment;
            if (request.getId() != null) {
                segment = existingById.get(request.getId());
                if (segment == null) {
                    throw new ParroTalkException("Segment does not belong to lesson", HttpStatusCode.valueOf(400));
                }
                if (!Objects.equals(segment.getText(), request.getText().trim())) {
                    changedTextSegmentIds.add(segment.getId());
                }
            } else {
                segment = new TranscriptionSegment();
                segment.setLesson(lesson);
                hasNewSegments.set(true);
            }

            segment.setText(request.getText().trim());
            segment.setStartTime(request.getStartTime());
            segment.setEndTime(request.getEndTime());
            segment.setDisplayOrder(request.getOrder());
            return segment;
        }).collect(Collectors.toList());

        transcriptionSegmentRepository.saveAll(upserts);
        if (!changedTextSegmentIds.isEmpty()) {
            segmentTranslationRepository.deleteBySegmentIdInAndTargetLanguage(
                    changedTextSegmentIds,
                    TranslationService.DEFAULT_TARGET_LANGUAGE);
        }
        if (hasNewSegments.get() || !changedTextSegmentIds.isEmpty()) {
            scheduleTranslationAfterCommit(lessonId);
        }

        return getAdminLessonDetail(lessonId);
    }

    /**
     * Get accessible lesson detail.
     * 
     * @param lessonId Lesson ID
     * @param user     User
     * @return Lesson
     */
    public LessonResponse getAccessibleLessonDetail(UUID lessonId, User user) {
        Lesson lesson = findAccessibleLesson(lessonId, user);

        List<TranscriptionSegment> lessonSegments = transcriptionSegmentRepository
                .findByLessonIdOrderByDisplayOrderAsc(lessonId);
        List<TranscriptionResponse> segments = buildSegmentResponses(lessonSegments);

        LessonResponse response = lessonMapper.toLessonResponse(lesson);
        response.setSegments(segments);
        response.setTranslationSummary(buildTranslationSummary(lessonId, lessonSegments.size()));
        return response;
    }

    /**
     * Trigger background generation for translations missing from an admin-managed lesson.
     *
     * @param lessonId Lesson ID
     * @return Current translation summary
     */
    public TranslationSummaryResponse requestAdminTranslationGeneration(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));
        return requestTranslationGeneration(lesson);
    }

    /**
     * Generate or regenerate one segment translation for an admin-managed lesson.
     *
     * @param lessonId  Lesson ID
     * @param segmentId Segment ID
     * @return Generated translation
     */
    public SegmentTranslationResponse requestAdminSegmentTranslationGeneration(UUID lessonId, UUID segmentId) {
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));
        TranscriptionSegment segment = transcriptionSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new ParroTalkException("Segment not found", HttpStatusCode.valueOf(404)));
        if (!lessonId.equals(segment.getLesson().getId())) {
            throw new ParroTalkException("Segment does not belong to lesson", HttpStatusCode.valueOf(400));
        }
        return translationService.translateSegment(segmentId, TranslationService.DEFAULT_TARGET_LANGUAGE);
    }

    /**
     * Validate segments.
     * 
     * @param segments List of segments.
     * @throws ParroTalkException if segments are invalid.
     */
    private void validateSegments(List<UpsertSegmentRequest> segments) {

        // Sort segments by start time
        List<UpsertSegmentRequest> sortedByStart = segments.stream()
                .sorted(Comparator.comparing(UpsertSegmentRequest::getStartTime))
                .collect(Collectors.toList());

        // Validate each segment to ensure startTime is less than endTime
        for (UpsertSegmentRequest segment : segments) {
            if (segment.getStartTime() >= segment.getEndTime()) {
                throw new ParroTalkException("startTime must be less than endTime", HttpStatusCode.valueOf(400));
            }
        }

        // Validate each segment to ensure no overlap
        for (int i = 1; i < sortedByStart.size(); i++) {
            if (sortedByStart.get(i - 1).getEndTime() > sortedByStart.get(i).getStartTime()) {
                throw new ParroTalkException("segments must not overlap", HttpStatusCode.valueOf(400));
            }
        }
    }

    /**
     * Find lesson by id.
     * 
     * @param lessonId Lesson id
     * @return Lesson
     */
    public Optional<Lesson> findById(UUID lessonId) {
        return lessonRepository.findById(lessonId);
    }

    /**
     * Save lesson.
     * 
     * @param lesson Lesson
     */
    @Transactional
    public void save(Lesson lesson) {
        lessonRepository.save(lesson);
    }

    /**
     * Find all broken lessons.
     * 
     * @return List of broken lessons
     */
    public List<Lesson> findAllBrokenLessons() {
        // One hour before now
        LocalDateTime tenHourBefore = LocalDateTime.now().minusHours(10);
        // // Pageable with 50 items per page
        // Pageable pageable = PageRequest.of(0, 50);
        // // Find all lessons with status PROCESSING and created at before one hour
        // before
        // // now
        // return
        // lessonRepository.findAllByStatusAndCreatedAtBefore(LessonStatus.PROCESSING,
        // oneHourBefore, pageable)
        // .getContent();
        // return lessonRepository.findAll();
        return lessonRepository.findAllByStatusAndCreatedAtBefore(LessonStatus.DONE, tenHourBefore);
    }

    /**
     * Find lesson by id for update.
     *
     * @param lessonId Lesson id
     * @return Lesson
     * @throws ParroTalkException if lesson not found
     */
    public Lesson findLessonForUpdate(UUID lessonId) {
        return lessonRepository.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatus.NOT_FOUND));
    }

    private List<TranscriptionResponse> buildSegmentResponses(List<TranscriptionSegment> segments) {
        if (segments.isEmpty()) {
            return List.of();
        }

        List<UUID> segmentIds = segments.stream()
                .map(TranscriptionSegment::getId)
                .toList();
        Map<UUID, SegmentTranslationResponse> translationsBySegmentId = segmentTranslationRepository
                .findBySegmentIdInAndTargetLanguage(segmentIds, TranslationService.DEFAULT_TARGET_LANGUAGE)
                .stream()
                .collect(Collectors.toMap(
                        translation -> translation.getSegment().getId(),
                        translation -> new SegmentTranslationResponse(
                                translation.getTargetLanguage(),
                                translation.getTranslatedText())));

        return segments.stream()
                .map(segment -> TranscriptionResponse.builder()
                        .id(segment.getId())
                        .text(segment.getText())
                        .start(segment.getStartTime())
                        .end(segment.getEndTime())
                        .order(segment.getDisplayOrder())
                        .difficulty(segment.getDifficulty())
                        .translation(translationsBySegmentId.get(segment.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    private TranslationSummaryResponse buildTranslationSummary(UUID lessonId, int totalSegments) {
        long translatedCount = segmentTranslationRepository.countBySegmentLessonIdAndTargetLanguage(
                lessonId,
                TranslationService.DEFAULT_TARGET_LANGUAGE);
        TranslationStatus status = resolveTranslationStatus(translatedCount, totalSegments);
        return new TranslationSummaryResponse(
                TranslationService.DEFAULT_TARGET_LANGUAGE,
                status,
                translatedCount,
                totalSegments);
    }

    TranslationStatus resolveTranslationStatus(long translatedCount, int totalSegments) {
        return translatedCount == 0
                ? TranslationStatus.NOT_STARTED
                : translatedCount >= totalSegments
                        ? TranslationStatus.COMPLETED
                        : TranslationStatus.PARTIAL;
    }

    private void scheduleTranslationAfterCommit(UUID lessonId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                translationService.translateLessonSegmentsAsync(
                        lessonId,
                        TranslationService.DEFAULT_TARGET_LANGUAGE);
            }
        });
    }

    private Lesson findAccessibleLesson(UUID lessonId, User user) {
        return lessonRepository.findByIdAndVisibilityStatus(lessonId, LessonVisibilityStatus.PUBLISHED)
                .or(() -> lessonRepository.findByIdAndOwnerId(lessonId, user.getId()))
                .orElseThrow(() -> new ParroTalkException("Lesson not found", HttpStatusCode.valueOf(404)));
    }

    private TranslationSummaryResponse requestTranslationGeneration(Lesson lesson) {
        List<TranscriptionSegment> segments = transcriptionSegmentRepository
                .findByLessonIdOrderByDisplayOrderAsc(lesson.getId());
        TranslationSummaryResponse summary = buildTranslationSummary(lesson.getId(), segments.size());
        if (summary.status() != TranslationStatus.COMPLETED && !segments.isEmpty()) {
            translationService.translateLessonSegmentsAsync(
                    lesson.getId(),
                    TranslationService.DEFAULT_TARGET_LANGUAGE);
        }
        return summary;
    }
}

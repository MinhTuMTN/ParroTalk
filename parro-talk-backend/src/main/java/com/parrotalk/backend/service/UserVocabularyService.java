package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.VocabularyStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.dictionary.SaveVocabularyRequest;
import com.parrotalk.backend.dto.dictionary.UpdateVocabularyRequest;
import com.parrotalk.backend.dto.dictionary.UserVocabularyResponse;
import com.parrotalk.backend.dto.dictionary.VocabularyOccurrenceResponse;
import com.parrotalk.backend.entity.DictionaryEntry;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserVocabulary;
import com.parrotalk.backend.entity.UserVocabularyOccurrence;
import com.parrotalk.backend.repository.DictionaryEntryRepository;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.repository.UserVocabularyOccurrenceRepository;
import com.parrotalk.backend.repository.UserVocabularyRepository;
import com.parrotalk.backend.util.WordNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserVocabularyService {

    private final UserVocabularyRepository userVocabularyRepository;
    private final UserVocabularyOccurrenceRepository occurrenceRepository;
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final LessonRepository lessonRepository;
    private final TranscriptionSegmentRepository segmentRepository;
    private final WordNormalizer wordNormalizer;

    @Transactional
    public UserVocabularyResponse save(User user, SaveVocabularyRequest request) {
        requireUser(user);
        String normalizedWord = wordNormalizer.normalize(request.word());
        if (normalizedWord.isBlank()) {
            throw new IllegalArgumentException("word must not be blank");
        }

        UserVocabulary vocabulary = userVocabularyRepository
                .findByUserIdAndNormalizedWord(user.getId(), normalizedWord)
                .orElseGet(() -> createVocabulary(user, request, normalizedWord));

        maybeAddOccurrence(vocabulary, request);
        return toResponse(vocabulary, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserVocabularyResponse> list(User user, VocabularyStatus status, int page, int size) {
        requireUser(user);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<UserVocabulary> result = status == null
                ? userVocabularyRepository.findByUserId(user.getId(), pageable)
                : userVocabularyRepository.findByUserIdAndStatus(user.getId(), status, pageable);

        return PageResponse.<UserVocabularyResponse>builder()
                .content(result.getContent().stream().map(vocabulary -> toResponse(vocabulary, false)).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public UserVocabularyResponse update(User user, UUID vocabularyId, UpdateVocabularyRequest request) {
        requireUser(user);
        UserVocabulary vocabulary = findOwnedVocabulary(user, vocabularyId);

        if (request.status() != null) {
            vocabulary.setStatus(request.status());
        }
        if (request.difficulty() != null) {
            vocabulary.setDifficulty(request.difficulty());
        }
        if (request.note() != null) {
            vocabulary.setNote(request.note());
        }
        if (request.nextReviewAt() != null) {
            vocabulary.setNextReviewAt(request.nextReviewAt());
        }

        return toResponse(userVocabularyRepository.save(vocabulary), true);
    }

    @Transactional
    public UserVocabularyResponse archive(User user, UUID vocabularyId) {
        requireUser(user);
        UserVocabulary vocabulary = findOwnedVocabulary(user, vocabularyId);
        vocabulary.setStatus(VocabularyStatus.ARCHIVED);
        return toResponse(userVocabularyRepository.save(vocabulary), true);
    }

    private UserVocabulary createVocabulary(User user, SaveVocabularyRequest request, String normalizedWord) {
        DictionaryEntry dictionaryEntry = request.dictionaryEntryId() == null
                ? null
                : dictionaryEntryRepository.findById(request.dictionaryEntryId()).orElse(null);

        UserVocabulary vocabulary = UserVocabulary.builder()
                .user(user)
                .normalizedWord(normalizedWord)
                .displayWord(resolveDisplayWord(request, normalizedWord))
                .dictionaryEntry(dictionaryEntry)
                .note(request.note())
                .status(VocabularyStatus.NEW)
                .build();

        try {
            return userVocabularyRepository.save(vocabulary);
        } catch (DataIntegrityViolationException ex) {
            return userVocabularyRepository.findByUserIdAndNormalizedWord(user.getId(), normalizedWord)
                    .orElseThrow(() -> ex);
        }
    }

    private void maybeAddOccurrence(UserVocabulary vocabulary, SaveVocabularyRequest request) {
        if (request.lessonId() == null && request.segmentId() == null && request.contextText() == null) {
            return;
        }

        if (request.lessonId() != null && request.segmentId() != null
                && occurrenceRepository.existsByUserVocabularyIdAndLessonIdAndSegmentIdAndWord(
                vocabulary.getId(), request.lessonId(), request.segmentId(), request.word())) {
            return;
        }

        Lesson lesson = request.lessonId() == null ? null : lessonRepository.findById(request.lessonId()).orElse(null);
        TranscriptionSegment segment = request.segmentId() == null ? null : segmentRepository.findById(request.segmentId()).orElse(null);

        UserVocabularyOccurrence occurrence = UserVocabularyOccurrence.builder()
                .userVocabulary(vocabulary)
                .lesson(lesson)
                .segment(segment)
                .word(request.word())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .contextText(request.contextText())
                .build();

        occurrenceRepository.save(occurrence);
    }

    private UserVocabulary findOwnedVocabulary(User user, UUID vocabularyId) {
        UserVocabulary vocabulary = userVocabularyRepository.findById(vocabularyId)
                .orElseThrow(() -> new IllegalArgumentException("vocabulary not found"));
        if (!Objects.equals(vocabulary.getUser().getId(), user.getId())) {
            throw new IllegalArgumentException("vocabulary not found");
        }
        return vocabulary;
    }

    private UserVocabularyResponse toResponse(UserVocabulary vocabulary, boolean includeOccurrences) {
        List<VocabularyOccurrenceResponse> occurrences = includeOccurrences
                ? occurrenceRepository.findByUserVocabularyIdOrderByCreatedAtDesc(vocabulary.getId())
                .stream()
                .map(this::toOccurrenceResponse)
                .toList()
                : List.of();

        return new UserVocabularyResponse(
                vocabulary.getId(),
                vocabulary.getNormalizedWord(),
                vocabulary.getDisplayWord(),
                vocabulary.getNote(),
                vocabulary.getStatus(),
                vocabulary.getDifficulty(),
                vocabulary.getReviewCount(),
                vocabulary.getLastReviewedAt(),
                vocabulary.getNextReviewAt(),
                vocabulary.getCreatedAt(),
                occurrences
        );
    }

    private VocabularyOccurrenceResponse toOccurrenceResponse(UserVocabularyOccurrence occurrence) {
        UUID lessonId = occurrence.getLesson() == null ? null : occurrence.getLesson().getId();
        UUID segmentId = occurrence.getSegment() == null ? null : occurrence.getSegment().getId();
        return new VocabularyOccurrenceResponse(
                occurrence.getId(),
                lessonId,
                segmentId,
                occurrence.getWord(),
                occurrence.getStartTime(),
                occurrence.getEndTime(),
                occurrence.getContextText(),
                occurrence.getCreatedAt()
        );
    }

    private String resolveDisplayWord(SaveVocabularyRequest request, String normalizedWord) {
        if (request.displayWord() != null && !request.displayWord().isBlank()) {
            return request.displayWord().trim();
        }
        return request.word() == null || request.word().isBlank() ? normalizedWord : request.word().trim();
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("authenticated user is required");
        }
    }
}

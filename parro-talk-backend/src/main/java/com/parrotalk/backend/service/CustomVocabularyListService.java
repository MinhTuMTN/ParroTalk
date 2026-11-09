package com.parrotalk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.UserNotificationType;
import com.parrotalk.backend.constant.VocabularyCrawlStatus;
import com.parrotalk.backend.constant.VocabularyListStatus;
import com.parrotalk.backend.constant.VocabularyListVisibility;
import com.parrotalk.backend.dto.vocablist.AddVocabularyListItemRequest;
import com.parrotalk.backend.dto.vocablist.CreateVocabularyListRequest;
import com.parrotalk.backend.dto.vocablist.CustomListPracticeQuestionDto;
import com.parrotalk.backend.dto.vocablist.GenerateMoreQuestionsRequest;
import com.parrotalk.backend.dto.vocablist.ShareVocabularyListRequest;
import com.parrotalk.backend.dto.vocablist.SubmitCustomListAnswerRequest;
import com.parrotalk.backend.dto.vocablist.UpdateVocabularyListItemSelectionsRequest;
import com.parrotalk.backend.dto.vocablist.VocabularyListDetailDto;
import com.parrotalk.backend.dto.vocablist.VocabularyListItemDto;
import com.parrotalk.backend.dto.vocablist.VocabularyListSummaryDto;
import com.parrotalk.backend.entity.GrammarMultipleChoiceQuestion;
import com.parrotalk.backend.entity.Idiom;
import com.parrotalk.backend.entity.PhrasalVerb;
import com.parrotalk.backend.entity.SenseAntonym;
import com.parrotalk.backend.entity.SenseCollocation;
import com.parrotalk.backend.entity.SenseSynonym;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserVocabularyListProgress;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyList;
import com.parrotalk.backend.entity.VocabularyListItem;
import com.parrotalk.backend.entity.VocabularyListShare;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.GrammarMultipleChoiceQuestionRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserVocabularyListProgressRepository;
import com.parrotalk.backend.repository.VocabularyListItemRepository;
import com.parrotalk.backend.repository.VocabularyListRepository;
import com.parrotalk.backend.repository.VocabularyListShareRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomVocabularyListService {

    private final VocabularyListRepository listRepository;
    private final VocabularyListItemRepository itemRepository;
    private final VocabularyListShareRepository shareRepository;
    private final UserVocabularyListProgressRepository progressRepository;
    private final VocabularyRepository vocabularyRepository;
    private final GrammarMultipleChoiceQuestionRepository mcqRepository;
    private final UserRepository userRepository;
    private final VocabCrawlTaskProducer crawlTaskProducer;
    private final CustomListQuestionBatchService questionBatchService;
    private final UserNotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public VocabularyListSummaryDto createList(User user, CreateVocabularyListRequest request) {
        requireUser(user);
        VocabularyListVisibility visibility = VocabularyListVisibility.PRIVATE;
        if (request.visibility() != null && !request.visibility().isBlank()) {
            try {
                visibility = VocabularyListVisibility.valueOf(request.visibility().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        VocabularyList list = VocabularyList.builder()
                .owner(user)
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .visibility(visibility)
                .status(VocabularyListStatus.DRAFT)
                .shareToken(generateShareToken())
                .build();

        VocabularyList saved = listRepository.save(list);
        return toSummaryDto(saved, user, 0);
    }

    @Transactional(readOnly = true)
    public List<VocabularyListSummaryDto> getMyLists(User user) {
        requireUser(user);
        List<VocabularyList> myLists = listRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
        return myLists.stream()
                .map(l -> toSummaryDto(l, user, l.getItems() != null ? l.getItems().size() : 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VocabularyListSummaryDto> getSharedWithMeLists(User user) {
        requireUser(user);
        List<VocabularyListShare> shares = shareRepository.findBySharedWithUserIdOrderByCreatedAtDesc(user.getId());
        return shares.stream()
                .map(VocabularyListShare::getVocabularyList)
                .filter(java.util.Objects::nonNull)
                .map(l -> toSummaryDto(l, user, l.getItems() != null ? l.getItems().size() : 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<VocabularyListSummaryDto>> getMyAndSharedLists(User user) {
        requireUser(user);
        List<VocabularyList> myLists = listRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
        List<VocabularyListSummaryDto> myDtos = myLists.stream()
                .map(l -> toSummaryDto(l, user, l.getItems() != null ? l.getItems().size() : 0))
                .toList();

        List<VocabularyListShare> shares = shareRepository.findBySharedWithUserIdOrderByCreatedAtDesc(user.getId());
        List<VocabularyListSummaryDto> sharedDtos = shares.stream()
                .map(VocabularyListShare::getVocabularyList)
                .filter(java.util.Objects::nonNull)
                .map(l -> toSummaryDto(l, user, l.getItems() != null ? l.getItems().size() : 0))
                .toList();

        return Map.of(
                "myLists", myDtos,
                "sharedWithMe", sharedDtos
        );
    }

    @Transactional(readOnly = true)
    public VocabularyListDetailDto getListDetail(User user, UUID listId, String shareToken) {
        requireUser(user);
        VocabularyList list = findAccessibleList(user, listId, shareToken);
        List<VocabularyListItem> items = itemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(listId);
        List<VocabularyListShare> shares = shareRepository.findByVocabularyListIdOrderByCreatedAtDesc(listId);
        List<UserVocabularyListProgress> progresses = progressRepository.findByUserIdAndVocabularyListId(user.getId(), listId);

        Map<UUID, UserVocabularyListProgress> progressByItemId = new HashMap<>();
        for (UserVocabularyListProgress p : progresses) {
            if (p.getListItem() != null && p.getListItem().getId() != null) {
                progressByItemId.put(p.getListItem().getId(), p);
            }
        }

        List<VocabularyListItemDto> itemDtos = items.stream()
                .map(item -> toItemDto(item, progressByItemId.get(item.getId())))
                .toList();

        List<VocabularyListDetailDto.SharedUserDto> sharedUserDtos = shares.stream()
                .filter(s -> s.getSharedWithUser() != null)
                .map(s -> new VocabularyListDetailDto.SharedUserDto(
                        s.getSharedWithUser().getId(),
                        s.getSharedWithUser().getEmail(),
                        s.getSharedWithUser().getDisplayUsername(),
                        s.getSharedWithUser().getFullName(),
                        s.getRole()
                ))
                .toList();

        boolean isOwner = list.getOwner() != null && user.getId().equals(list.getOwner().getId());
        return new VocabularyListDetailDto(
                list.getId(),
                list.getTitle(),
                list.getDescription(),
                list.getVisibility().name(),
                list.getStatus().name(),
                isOwner || list.getVisibility() == VocabularyListVisibility.SHARED_LINK ? list.getShareToken() : null,
                list.getClonedFromListId(),
                list.getOwner() != null ? list.getOwner().getFullName() : "",
                isOwner,
                sharedUserDtos,
                itemDtos,
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<VocabularyListItemDto> searchWords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String cleanQuery = query.trim().toLowerCase();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Vocabulary> page = vocabularyRepository.findByNormalizedWordStartingWith(cleanQuery, pageable);

        return page.getContent().stream()
                .map(v -> {
                    VocabularyListItem mockItem = VocabularyListItem.builder()
                            .id(UUID.randomUUID())
                            .vocabulary(v)
                            .rawWord(v.getWord())
                            .normalizedWord(v.getNormalizedWord())
                            .sourceOption("DB_EXISTING")
                            .crawlStatus(VocabularyCrawlStatus.COMPLETED)
                            .build();
                    return toItemDto(mockItem, null);
                })
                .toList();
    }

    @Transactional
    public List<VocabularyListItemDto> addWordsToList(User user, UUID listId, AddVocabularyListItemRequest request) {
        requireUser(user);
        VocabularyList list = requireOwnedList(user, listId);

        String option = (request.sourceOption() != null && !request.sourceOption().isBlank())
                ? request.sourceOption().trim().toUpperCase()
                : "DB_EXISTING";
        boolean forceExternalCrawl = "EXTERNAL_CRAWL".equals(option);

        List<VocabularyListItemDto> addedDtos = new ArrayList<>();

        for (String rawInput : request.words()) {
            if (rawInput == null || rawInput.isBlank()) {
                continue;
            }
            String cleanWord = rawInput.trim();
            String normalizedWord = cleanWord.toLowerCase();

            Optional<VocabularyListItem> existingItemOpt = itemRepository.findByVocabularyListIdAndNormalizedWord(listId, normalizedWord);
            if (existingItemOpt.isPresent()) {
                VocabularyListItem existingItem = existingItemOpt.get();
                if (forceExternalCrawl) {
                    existingItem.setSourceOption("EXTERNAL_CRAWL");
                    existingItem.setCrawlStatus(VocabularyCrawlStatus.PENDING_CRAWL);
                    existingItem = itemRepository.save(existingItem);
                    crawlTaskProducer.sendCrawlTask(listId, existingItem.getId(), user.getId(), cleanWord);
                }
                addedDtos.add(toItemDto(existingItem, null));
                continue;
            }

            List<Vocabulary> dbMatches = forceExternalCrawl
                    ? List.of()
                    : vocabularyRepository.findByNormalizedWordIn(List.of(normalizedWord));

            if (!dbMatches.isEmpty()) {
                Vocabulary vocab = dbMatches.get(0);
                VocabularyListItem item = VocabularyListItem.builder()
                        .vocabularyList(list)
                        .vocabulary(vocab)
                        .rawWord(cleanWord)
                        .normalizedWord(normalizedWord)
                        .sourceOption("DB_EXISTING")
                        .crawlStatus(VocabularyCrawlStatus.COMPLETED)
                        .userNote(request.userNote())
                        .build();
                item.setSelectedSenseIds(VocabularyEnrichmentAiService.selectDefaultSenseIds(vocab.getSenses()));
                item.setSelectedIdiomIds(vocab.getIdioms().stream().map(Idiom::getId).filter(java.util.Objects::nonNull).toList());
                item.setSelectedPhrasalVerbIds(vocab.getPhrasalVerbs().stream().map(PhrasalVerb::getId).filter(java.util.Objects::nonNull).toList());

                VocabularyListItem savedItem = itemRepository.save(item);
                addedDtos.add(toItemDto(savedItem, null));
            } else {
                VocabularyListItem item = VocabularyListItem.builder()
                        .vocabularyList(list)
                        .rawWord(cleanWord)
                        .normalizedWord(normalizedWord)
                        .sourceOption("EXTERNAL_CRAWL")
                        .crawlStatus(VocabularyCrawlStatus.PENDING_CRAWL)
                        .userNote(request.userNote())
                        .build();
                VocabularyListItem savedItem = itemRepository.save(item);
                crawlTaskProducer.sendCrawlTask(listId, savedItem.getId(), user.getId(), cleanWord);
                addedDtos.add(toItemDto(savedItem, null));
            }
        }

        return addedDtos;
    }

    @Transactional
    public VocabularyListItemDto updateItemSelections(
            User user,
            UUID listId,
            UUID itemId,
            UpdateVocabularyListItemSelectionsRequest request) {
        requireUser(user);
        requireOwnedList(user, listId);
        VocabularyListItem item = itemRepository.findById(itemId)
                .filter(i -> i.getVocabularyList() != null && listId.equals(i.getVocabularyList().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List item not found"));

        if (request.selectedSenseIds() != null) {
            item.setSelectedSenseIds(request.selectedSenseIds());
        }
        if (request.selectedIdiomIds() != null) {
            item.setSelectedIdiomIds(request.selectedIdiomIds());
        }
        if (request.selectedPhrasalVerbIds() != null) {
            item.setSelectedPhrasalVerbIds(request.selectedPhrasalVerbIds());
        }
        if (request.userNote() != null) {
            item.setUserNote(request.userNote());
        }

        VocabularyListItem saved = itemRepository.save(item);
        return toItemDto(saved, null);
    }

    @Transactional
    public void deleteListItem(User user, UUID listId, UUID itemId) {
        requireUser(user);
        requireOwnedList(user, listId);
        VocabularyListItem item = itemRepository.findById(itemId)
                .filter(i -> i.getVocabularyList() != null && listId.equals(i.getVocabularyList().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List item not found"));
        itemRepository.delete(item);
    }

    @Transactional
    public Map<String, Object> shareList(User user, UUID listId, ShareVocabularyListRequest request) {
        requireUser(user);
        VocabularyList list = requireOwnedList(user, listId);

        if (Boolean.TRUE.equals(request.enableShareLink())) {
            list.setVisibility(VocabularyListVisibility.SHARED_LINK);
            if (list.getShareToken() == null || list.getShareToken().isBlank()) {
                list.setShareToken(generateShareToken());
            }
            listRepository.save(list);
        }

        if (request.emailOrUsername() != null && !request.emailOrUsername().isBlank()) {
            String targetKey = request.emailOrUsername().trim();
            User targetUser = userRepository.findByEmailIgnoreCase(targetKey)
                    .or(() -> userRepository.findByDisplayUsernameIgnoreCase(targetKey))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng: " + targetKey));

            if (!targetUser.getId().equals(user.getId())
                    && !shareRepository.existsByVocabularyListIdAndSharedWithUserId(listId, targetUser.getId())) {
                VocabularyListShare share = VocabularyListShare.builder()
                        .vocabularyList(list)
                        .sharedWithUser(targetUser)
                        .grantedByUser(user)
                        .role("VIEWER")
                        .build();
                shareRepository.save(share);

                notificationService.notifyUser(
                        targetUser,
                        UserNotificationType.LIST_SHARED_WITH_YOU,
                        user.getFullName() + " đã chia sẻ danh sách từ vựng với bạn",
                        "Bạn đã được cấp quyền xem và luyện tập danh sách '" + list.getTitle() + "'.",
                        list.getId(),
                        Map.of("listId", list.getId().toString(), "title", list.getTitle())
                );
            }
        }

        return Map.of(
                "listId", list.getId(),
                "visibility", list.getVisibility().name(),
                "shareToken", list.getShareToken() != null ? list.getShareToken() : ""
        );
    }

    @Transactional
    public VocabularyListDetailDto cloneList(User user, UUID sourceListId, String shareToken) {
        requireUser(user);
        VocabularyList sourceList = findAccessibleList(user, sourceListId, shareToken);
        List<VocabularyListItem> sourceItems = itemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(sourceListId);

        VocabularyList clonedList = VocabularyList.builder()
                .owner(user)
                .title(sourceList.getTitle() + " (Bản sao)")
                .description(sourceList.getDescription())
                .visibility(VocabularyListVisibility.PRIVATE)
                .status(VocabularyListStatus.ACTIVE)
                .shareToken(generateShareToken())
                .clonedFromListId(sourceList.getId())
                .build();
        clonedList = listRepository.save(clonedList);

        List<VocabularyListItem> clonedItems = new ArrayList<>();
        for (VocabularyListItem src : sourceItems) {
            VocabularyListItem copy = VocabularyListItem.builder()
                    .vocabularyList(clonedList)
                    .vocabulary(src.getVocabulary())
                    .rawWord(src.getRawWord())
                    .normalizedWord(src.getNormalizedWord())
                    .sourceOption(src.getSourceOption())
                    .crawlStatus(src.getCrawlStatus())
                    .userNote(src.getUserNote())
                    .displayOrder(src.getDisplayOrder())
                    .selectedSenseIdsJson(src.getSelectedSenseIdsJson())
                    .selectedIdiomIdsJson(src.getSelectedIdiomIdsJson())
                    .selectedPhrasalVerbIdsJson(src.getSelectedPhrasalVerbIdsJson())
                    .build();
            clonedItems.add(copy);
        }
        itemRepository.saveAll(clonedItems);

        return new VocabularyListDetailDto(
                clonedList.getId(),
                clonedList.getTitle(),
                clonedList.getDescription(),
                clonedList.getVisibility().name(),
                clonedList.getStatus().name(),
                clonedList.getShareToken(),
                clonedList.getClonedFromListId(),
                user.getFullName(),
                true,
                List.of(),
                clonedItems.stream().map(i -> toItemDto(i, null)).toList(),
                clonedList.getCreatedAt(),
                clonedList.getUpdatedAt()
        );
    }

    @Transactional
    public Map<String, Object> finalizeAndGenerateQuestions(User user, UUID listId) {
        requireUser(user);
        VocabularyList list = requireOwnedList(user, listId);
        list.setStatus(VocabularyListStatus.GENERATING_QUESTIONS);
        listRepository.save(list);
        questionBatchService.triggerFinalizeBatchAsync(listId, user.getId());
        return Map.of(
                "listId", listId,
                "status", VocabularyListStatus.GENERATING_QUESTIONS.name(),
                "message", "Đang chạy Batch AI tạo câu hỏi trắc nghiệm ở background. Hệ thống sẽ thông báo khi hoàn tất."
        );
    }

    @Transactional
    public Map<String, Object> generateMoreQuestions(User user, UUID listId, GenerateMoreQuestionsRequest request) {
        requireUser(user);
        requireOwnedList(user, listId);
        int count = request.count() != null ? request.count() : 4;
        int generated = 0;

        if (request.senseId() != null) {
            generated += questionBatchService.generateMoreForSenseSync(listId, user.getId(), request.senseId(), count);
        } else if (request.idiomId() != null) {
            generated += questionBatchService.generateMoreForIdiomSync(listId, user.getId(), request.idiomId(), count);
        } else if (request.phrasalVerbId() != null) {
            generated += questionBatchService.generateMoreForPhrasalVerbSync(listId, user.getId(), request.phrasalVerbId(), count);
        } else {
            List<VocabularyListItem> items = itemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(listId);
            for (VocabularyListItem item : items) {
                for (UUID sId : item.getSelectedSenseIds()) {
                    generated += questionBatchService.generateMoreForSenseSync(listId, user.getId(), sId, 2);
                }
            }
        }

        return Map.of(
                "listId", listId,
                "generatedCount", generated,
                "generationSource", "USER_LIST_MORE_AI"
        );
    }

    @Transactional(readOnly = true)
    public List<CustomListPracticeQuestionDto> getPracticeQuestions(
            User user,
            UUID listId,
            String shareToken,
            String category,
            int limit) {
        requireUser(user);
        findAccessibleList(user, listId, shareToken);
        List<VocabularyListItem> items = itemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(listId);

        Set<UUID> senseIds = new LinkedHashSet<>();
        Set<UUID> idiomIds = new LinkedHashSet<>();
        Set<UUID> pvIds = new LinkedHashSet<>();
        for (VocabularyListItem item : items) {
            senseIds.addAll(item.getSelectedSenseIds());
            idiomIds.addAll(item.getSelectedIdiomIds());
            pvIds.addAll(item.getSelectedPhrasalVerbIds());
        }

        List<GrammarMultipleChoiceQuestion> allQuestions = new ArrayList<>();
        if (!senseIds.isEmpty()) {
            allQuestions.addAll(mcqRepository.findBySenseIdIn(senseIds));
        }
        if (!idiomIds.isEmpty()) {
            allQuestions.addAll(mcqRepository.findByIdiomIdIn(idiomIds));
        }
        if (!pvIds.isEmpty()) {
            allQuestions.addAll(mcqRepository.findByPhrasalVerbIdIn(pvIds));
        }

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            allQuestions = allQuestions.stream()
                    .filter(q -> q.getQuestionCategory() != null && category.equalsIgnoreCase(q.getQuestionCategory().name()))
                    .toList();
        }

        List<GrammarMultipleChoiceQuestion> shuffled = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffled);
        int bounded = Math.max(1, Math.min(limit, 100));

        return shuffled.stream()
                .limit(bounded)
                .map(this::toPracticeQuestionDto)
                .toList();
    }

    @Transactional
    public void recordAnswerProgress(User user, UUID listId, SubmitCustomListAnswerRequest request) {
        requireUser(user);
        VocabularyList list = listRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        if (request.listItemId() == null) {
            return;
        }
        VocabularyListItem item = itemRepository.findById(request.listItemId()).orElse(null);
        if (item == null) {
            return;
        }

        UserVocabularyListProgress progress = progressRepository
                .findByUserIdAndListItemId(user.getId(), item.getId())
                .orElseGet(() -> UserVocabularyListProgress.builder()
                        .user(user)
                        .vocabularyList(list)
                        .listItem(item)
                        .masteryLevel("NEW")
                        .correctCount(0)
                        .wrongCount(0)
                        .build());

        if (request.isCorrect()) {
            progress.setCorrectCount(progress.getCorrectCount() + 1);
        } else {
            progress.setWrongCount(progress.getWrongCount() + 1);
        }
        int net = progress.getCorrectCount() - progress.getWrongCount();
        if (net >= 5) {
            progress.setMasteryLevel("MASTERED");
        } else if (progress.getCorrectCount() >= 2) {
            progress.setMasteryLevel("REVIEWING");
        } else {
            progress.setMasteryLevel("LEARNING");
        }
        progress.setLastPracticedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    private VocabularyList findAccessibleList(User user, UUID listId, String shareToken) {
        VocabularyList list = listRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh sách không tồn tại"));

        if (list.getOwner() != null && user.getId().equals(list.getOwner().getId())) {
            return list;
        }
        if (list.getVisibility() == VocabularyListVisibility.PUBLIC) {
            return list;
        }
        if (shareToken != null && !shareToken.isBlank() && shareToken.equals(list.getShareToken())) {
            return list;
        }
        if (shareRepository.existsByVocabularyListIdAndSharedWithUserId(listId, user.getId())) {
            return list;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập danh sách từ vựng này");
    }

    private VocabularyList requireOwnedList(User user, UUID listId) {
        VocabularyList list = listRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh sách không tồn tại"));
        if (list.getOwner() == null || !user.getId().equals(list.getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ sở hữu mới có quyền chỉnh sửa danh sách");
        }
        return list;
    }

    private static void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập");
        }
    }

    private static String generateShareToken() {
        return "pt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private VocabularyListSummaryDto toSummaryDto(VocabularyList list, User currentUser, int wordCount) {
        boolean isOwner = list.getOwner() != null && currentUser.getId().equals(list.getOwner().getId());
        return new VocabularyListSummaryDto(
                list.getId(),
                list.getTitle(),
                list.getDescription(),
                list.getVisibility().name(),
                list.getStatus().name(),
                isOwner ? list.getShareToken() : null,
                list.getOwner() != null ? list.getOwner().getFullName() : "",
                isOwner,
                wordCount,
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }

    private VocabularyListItemDto toItemDto(VocabularyListItem item, UserVocabularyListProgress progress) {
        Vocabulary vocab = item.getVocabulary();
        List<VocabularyListItemDto.SenseOptionDto> senses = new ArrayList<>();
        List<VocabularyListItemDto.PhraseOptionDto> idioms = new ArrayList<>();
        List<VocabularyListItemDto.PhraseOptionDto> pvs = new ArrayList<>();

        if (vocab != null) {
            for (VocabularySense s : vocab.getSenses()) {
                long qCount = s.getId() != null ? mcqRepository.countBySenseId(s.getId()) : 0L;
                senses.add(new VocabularyListItemDto.SenseOptionDto(
                        s.getId(),
                        s.getPartOfSpeech(),
                        s.getCefrLevel(),
                        s.getDefinitionEn(),
                        s.getDefinitionVn(),
                        s.getExampleEn(),
                        s.getExampleVn(),
                        s.getSynonyms() != null ? s.getSynonyms().stream().map(SenseSynonym::getSynonymWord).toList() : List.of(),
                        s.getAntonyms() != null ? s.getAntonyms().stream().map(SenseAntonym::getAntonymWord).toList() : List.of(),
                        s.getCollocations() != null ? s.getCollocations().stream().map(SenseCollocation::getCollocationWord).toList() : List.of(),
                        s.getTopic() != null ? s.getTopic().getName() : null,
                        s.getDataSource() != null ? s.getDataSource().name() : "IMPORT_JSON",
                        qCount
                ));
            }
            for (Idiom idm : vocab.getIdioms()) {
                long qCount = idm.getId() != null ? mcqRepository.countByIdiomId(idm.getId()) : 0L;
                idioms.add(new VocabularyListItemDto.PhraseOptionDto(
                        idm.getId(),
                        idm.getPhrase(),
                        idm.getDefinition(),
                        idm.getDefinitionVn(),
                        idm.getExample(),
                        idm.getExampleVn(),
                        idm.getDataSource() != null ? idm.getDataSource().name() : "IMPORT_JSON",
                        qCount
                ));
            }
            for (PhrasalVerb pv : vocab.getPhrasalVerbs()) {
                long qCount = pv.getId() != null ? mcqRepository.countByPhrasalVerbId(pv.getId()) : 0L;
                pvs.add(new VocabularyListItemDto.PhraseOptionDto(
                        pv.getId(),
                        pv.getPhrase(),
                        pv.getDefinition(),
                        pv.getDefinitionVn(),
                        pv.getExample(),
                        pv.getExampleVn(),
                        pv.getDataSource() != null ? pv.getDataSource().name() : "IMPORT_JSON",
                        qCount
                ));
            }
        }

        return new VocabularyListItemDto(
                item.getId(),
                vocab != null ? vocab.getId() : null,
                item.getRawWord(),
                item.getNormalizedWord(),
                vocab != null ? vocab.getCommonMeaningVn() : null,
                vocab != null && vocab.getDataSource() != null ? vocab.getDataSource().name() : null,
                item.getSourceOption(),
                item.getCrawlStatus().name(),
                item.getCrawlErrorMessage(),
                item.getUserNote(),
                item.getSelectedSenseIds(),
                item.getSelectedIdiomIds(),
                item.getSelectedPhrasalVerbIds(),
                senses,
                idioms,
                pvs,
                progress != null ? progress.getMasteryLevel() : "NEW",
                progress != null ? progress.getCorrectCount() : 0,
                progress != null ? progress.getWrongCount() : 0
        );
    }

    private CustomListPracticeQuestionDto toPracticeQuestionDto(GrammarMultipleChoiceQuestion q) {
        List<String> options = List.of();
        try {
            options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
        }
        return new CustomListPracticeQuestionDto(
                q.getId(),
                q.getVocabulary() != null ? q.getVocabulary().getId() : null,
                q.getSense() != null ? q.getSense().getId() : null,
                q.getIdiom() != null ? q.getIdiom().getId() : null,
                q.getPhrasalVerb() != null ? q.getPhrasalVerb().getId() : null,
                q.getVocabulary() != null ? q.getVocabulary().getWord() : "",
                q.getQuestionCategory() != null ? q.getQuestionCategory().name() : "GRAMMAR_PATTERN",
                q.getGrammarTopic(),
                q.getGenerationSource() != null ? q.getGenerationSource().name() : "PIPELINE_BATCH",
                q.getDifficulty(),
                q.getQuestionText(),
                options,
                q.getCorrectIndex() != null ? q.getCorrectIndex() : 0,
                q.getCorrectAnswer(),
                q.getCompletedSentence(),
                q.getExplanationEn(),
                q.getExplanationVn()
        );
    }
}


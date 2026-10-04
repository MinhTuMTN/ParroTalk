package com.parrotalk.backend.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.constant.VocabularyStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.TopicWordDto;
import com.parrotalk.backend.dto.VocabularyTopicSummaryDto;
import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.UserVocabularyRepository;
import com.parrotalk.backend.repository.VocabularyRepository;

import lombok.RequiredArgsConstructor;

/**
 * Topic detail use cases: topic summary and paginated words, addressed by topic id.
 * The current user is only ever supplied by the controller from the security context.
 */
@Service
@RequiredArgsConstructor
public class VocabularyTopicService {

    public static final int MAX_PAGE_SIZE = 50;

    private final TopicRepository topicRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final PublicVocabularyService publicVocabularyService;

    @Transactional(readOnly = true)
    public VocabularyTopicSummaryDto getTopicSummary(UUID topicId, User user) {
        Topic topic = requireTopic(topicId);
        long wordCount = topicRepository.countDistinctVocabulariesByTopicId(topicId);
        Long learned = null;
        if (user != null && user.getId() != null) {
            learned = userVocabularyRepository.countLearnedWordsForTopic(user.getId(), topicId);
        }
        // Always build a fresh DTO: never touch the cached topics-list DTOs.
        return VocabularyTopicSummaryDto.builder()
                .id(topic.getId())
                .name(topic.getName())
                .wordCount(wordCount)
                .learnedWordCount(learned)
                .description("Danh mục từ vựng " + topic.getName() + " chuẩn IELTS")
                .icon(publicVocabularyService.getTopicIcon(topic.getName()))
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<TopicWordDto> getTopicWords(UUID topicId, int page, int size, User user) {
        if (page < 0) {
            throw new ParroTalkException("page must be >= 0", ErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ParroTalkException("size must be between 1 and " + MAX_PAGE_SIZE,
                    ErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST);
        }
        requireTopic(topicId);

        Page<UUID> idPage = vocabularyRepository.findIdsByTopicId(topicId, PageRequest.of(page, size));
        List<UUID> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), idPage.getNumber(), idPage.getSize(),
                    idPage.getTotalElements(), idPage.getTotalPages());
        }

        Map<UUID, Vocabulary> byId = new HashMap<>();
        for (Vocabulary v : vocabularyRepository.findAllByIdWithSenses(ids)) {
            byId.put(v.getId(), v);
        }

        Map<String, String> statuses = loadStatuses(user, byId.values());

        // Preserve the ordering returned by the paged id query.
        List<TopicWordDto> content = ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(v -> toDto(v, topicId, statuses.get(v.getNormalizedWord())))
                .toList();

        return new PageResponse<>(content, idPage.getNumber(), idPage.getSize(),
                idPage.getTotalElements(), idPage.getTotalPages());
    }

    private Topic requireTopic(UUID topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new ParroTalkException("Topic not found", "TOPIC_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private Map<String, String> loadStatuses(User user, java.util.Collection<Vocabulary> vocabularies) {
        if (user == null || user.getId() == null || vocabularies.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> words = vocabularies.stream().map(Vocabulary::getNormalizedWord).toList();
        Map<String, String> result = new HashMap<>();
        for (Object[] row : userVocabularyRepository.findStatusesByUserIdAndNormalizedWordIn(user.getId(), words)) {
            VocabularyStatus status = (VocabularyStatus) row[1];
            if (status != null && status != VocabularyStatus.ARCHIVED) {
                result.put((String) row[0], status.name());
            }
        }
        return result;
    }

    private TopicWordDto toDto(Vocabulary v, UUID topicId, String status) {
        VocabularySense sense = v.getSenses().stream()
                .filter(s -> s.getTopic() != null && topicId.equals(s.getTopic().getId()))
                .findFirst()
                .orElse(v.getSenses().isEmpty() ? null : v.getSenses().get(0));

        String phonetic = null;
        String us = null;
        String uk = null;
        for (VocabularyPronunciation p : v.getPronunciations()) {
            if (phonetic == null) phonetic = p.getIpa();
            if ("US".equalsIgnoreCase(p.getAccent())) us = p.getAudioUrl();
            if ("UK".equalsIgnoreCase(p.getAccent())) uk = p.getAudioUrl();
        }

        return TopicWordDto.builder()
                .id(v.getId())
                .word(v.getWord())
                .commonMeaningVi(v.getCommonMeaningVn())
                .partOfSpeech(sense != null ? sense.getPartOfSpeech() : null)
                .cefrLevel(sense != null ? sense.getCefrLevel() : null)
                .phonetic(phonetic)
                .audioUsUrl(us)
                .audioUkUrl(uk)
                .status(status)
                .build();
    }
}

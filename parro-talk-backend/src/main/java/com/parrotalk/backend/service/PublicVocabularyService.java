package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.PublicVocabularyDetailDto;
import com.parrotalk.backend.dto.PublicVocabularySummaryDto;
import com.parrotalk.backend.dto.VocabularyReportRequestDto;
import com.parrotalk.backend.dto.VocabularyTopicSummaryDto;
import com.parrotalk.backend.entity.*;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.VocabularyReportRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.apache.commons.lang3.StringUtils;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.constant.ErrorCode;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;

import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for public vocabulary lookup, search, topics, detail, and reporting.
 *
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class PublicVocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final com.parrotalk.backend.repository.DictionaryEntryRepository dictionaryEntryRepository;
    private final VocabularyReportRepository vocabularyReportRepository;

    @Cacheable(value = "vocabularyTopics")
    @Transactional(readOnly = true)
    public List<VocabularyTopicSummaryDto> getTopics() {
        List<Object[]> rawTopics = topicRepository.findTopicsWithCounts();
        return rawTopics.stream()
                .map(row -> VocabularyTopicSummaryDto.builder()
                        .id((java.util.UUID) row[0])
                        .name((String) row[1])
                        .wordCount(((Number) row[2]).longValue())
                        .description("Danh mục từ vựng " + row[1] + " chuẩn IELTS")
                        .icon(getTopicIcon((String) row[1]))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicVocabularySummaryDto> searchVocabularies(
            String keyword, String topic, String cefrLevel, String partOfSpeech, int page, int size) {
        if (page < 0 || size < 1 || size > VocabularyTopicService.MAX_PAGE_SIZE) {
            throw new ParroTalkException("Invalid page (>=0) or size (1-" + VocabularyTopicService.MAX_PAGE_SIZE + ")",
                    ErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST);
        }
        Specification<Vocabulary> spec = buildSearchSpec(keyword, topic, cefrLevel, partOfSpeech);
        Page<Vocabulary> entries = vocabularyRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by("normalizedWord").ascending().and(Sort.by("id").ascending())));
        Page<PublicVocabularySummaryDto> dtos = entries.map(this::mapToSummaryDto);
        return new PageResponse<>(dtos.getContent(), dtos.getNumber(), dtos.getSize(), dtos.getTotalElements(), dtos.getTotalPages());
    }

    private Specification<Vocabulary> buildSearchSpec(String keyword, String topic, String cefrLevel, String partOfSpeech) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(keyword)) {
                String escaped = keyword.trim().toLowerCase()
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("normalizedWord")), "%" + escaped + "%", '\\'));
            }
            boolean hasTopic = StringUtils.isNotBlank(topic);
            boolean hasCefr = StringUtils.isNotBlank(cefrLevel);
            boolean hasPos = StringUtils.isNotBlank(partOfSpeech);
            if (hasTopic || hasCefr || hasPos) {
                // Same sense must satisfy all sense-level filters.
                Join<Vocabulary, VocabularySense> sense = root.join("senses");
                if (hasTopic) {
                    Join<VocabularySense, Topic> t = sense.join("topic");
                    predicates.add(cb.equal(cb.lower(t.get("name")), topic.trim().toLowerCase()));
                }
                if (hasCefr) {
                    predicates.add(cb.equal(sense.get("cefrLevel"), cefrLevel.trim()));
                }
                if (hasPos) {
                    predicates.add(cb.equal(sense.get("partOfSpeech"), partOfSpeech.trim()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public List<PublicVocabularySummaryDto> getFeatured() {
        List<Vocabulary> entries = vocabularyRepository.findRandomVocabularies(null, "B2", 10);
        return entries.stream().map(this::mapToSummaryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PublicVocabularyDetailDto getDetail(UUID id, User user) {
        Vocabulary entry = vocabularyRepository.findByIdWithGraph(id)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found with id: " + id));
        return mapToDetailDto(entry, false);
    }

    @Transactional(readOnly = true)
    public PublicVocabularyDetailDto getDetailByIdOrWord(String idOrWord, User user) {
        Vocabulary entry;
        try {
            UUID id = UUID.fromString(idOrWord);
            entry = vocabularyRepository.findByIdWithGraph(id).orElse(null);
        } catch (IllegalArgumentException e) {
            String normalized = idOrWord.toLowerCase().trim();
            entry = vocabularyRepository.findByNormalizedWord(normalized).orElse(null);
        }

        if (entry == null) {
            throw new RuntimeException("Vocabulary not found: " + idOrWord);
        }
        return mapToDetailDto(entry, false); // ignoring user saved status for now or check it
    }

    @Transactional
    public void submitReport(UUID id, User user, VocabularyReportRequestDto request) {
        Vocabulary entry = vocabularyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found"));

        VocabularyReport report = VocabularyReport.builder()
                .reporterId(user.getId())
                .dictionaryEntry(dictionaryEntryRepository.findFirstByNormalizedWord(entry.getNormalizedWord()).orElse(null))
                .reportType(request.getReportType())
                .reason(request.getReason())
                .description(request.getDescription())
                .build();
        vocabularyReportRepository.save(report);
    }

    private PublicVocabularySummaryDto mapToSummaryDto(Vocabulary entry) {
        String pos = entry.getSenses().isEmpty() ? null : entry.getSenses().get(0).getPartOfSpeech();
        String cefr = entry.getSenses().isEmpty() ? null : entry.getSenses().get(0).getCefrLevel();
        String topic = entry.getSenses().isEmpty() || entry.getSenses().get(0).getTopic() == null ? null : entry.getSenses().get(0).getTopic().getName();
        String phonetic = entry.getPronunciations().isEmpty() ? null : entry.getPronunciations().get(0).getIpa();
        String audioUsUrl = null;
        String audioUkUrl = null;
        for (VocabularyPronunciation p : entry.getPronunciations()) {
            if ("US".equalsIgnoreCase(p.getAccent())) audioUsUrl = p.getAudioUrl();
            if ("UK".equalsIgnoreCase(p.getAccent())) audioUkUrl = p.getAudioUrl();
        }

        return PublicVocabularySummaryDto.builder()
                .id(entry.getId())
                .word(entry.getWord())
                .commonMeaningVi(entry.getCommonMeaningVn())
                .partOfSpeech(pos)
                .phonetic(phonetic)
                .cefrLevel(cefr)
                .topic(topic)
                .audioUsUrl(audioUsUrl)
                .audioUkUrl(audioUkUrl)
                .build();
    }

    private PublicVocabularyDetailDto mapToDetailDto(Vocabulary entry, boolean isSaved) {
        String pos = entry.getSenses().isEmpty() ? null : entry.getSenses().get(0).getPartOfSpeech();
        String cefr = entry.getSenses().isEmpty() ? null : entry.getSenses().get(0).getCefrLevel();
        String topic = entry.getSenses().isEmpty() || entry.getSenses().get(0).getTopic() == null ? null : entry.getSenses().get(0).getTopic().getName();
        String phonetic = entry.getPronunciations().isEmpty() ? null : entry.getPronunciations().get(0).getIpa();
        String audioUsUrl = null;
        String audioUkUrl = null;
        for (VocabularyPronunciation p : entry.getPronunciations()) {
            if ("US".equalsIgnoreCase(p.getAccent())) audioUsUrl = p.getAudioUrl();
            if ("UK".equalsIgnoreCase(p.getAccent())) audioUkUrl = p.getAudioUrl();
        }

        List<PublicVocabularyDetailDto.SenseItemDto> senses = entry.getSenses().stream()
            .map(s -> PublicVocabularyDetailDto.SenseItemDto.builder()
                .id(s.getId().toString())
                .partOfSpeech(s.getPartOfSpeech())
                .cefrLevel(s.getCefrLevel())
                .topicTag(s.getTopic() != null ? s.getTopic().getName() : null)
                .definitionEn(s.getDefinitionEn())
                .definitionVn(s.getDefinitionVn())
                .exampleEn(s.getExampleEn())
                .exampleVn(s.getExampleVn())
                .synonyms(s.getSynonyms().stream().map(SenseSynonym::getSynonymWord).collect(Collectors.toList()))
                .collocations(s.getCollocations().stream().map(SenseCollocation::getCollocationWord).collect(Collectors.toList()))
                .build()
            ).collect(Collectors.toList());

        List<PublicVocabularyDetailDto.PhrasalVerbItemDto> phrasalVerbs = entry.getPhrasalVerbs().stream()
            .map(pv -> PublicVocabularyDetailDto.PhrasalVerbItemDto.builder()
                .phrasalVerb(pv.getPhrase())
                .definitionEn(pv.getDefinition())
                .definitionVn(pv.getDefinitionVn())
                .exampleEn(pv.getExample())
                .exampleVn(pv.getExampleVn())
                .build()
            ).collect(Collectors.toList());

        List<PublicVocabularyDetailDto.IdiomItemDto> idioms = entry.getIdioms().stream()
            .map(i -> PublicVocabularyDetailDto.IdiomItemDto.builder()
                .idiom(i.getPhrase())
                .definitionEn(i.getDefinition())
                .definitionVn(i.getDefinitionVn())
                .exampleEn(i.getExample())
                .exampleVn(i.getExampleVn())
                .build()
            ).collect(Collectors.toList());
            
        List<String> flatDefinitions = entry.getSenses().stream().map(VocabularySense::getDefinitionEn).collect(Collectors.toList());
        List<String> flatExamples = entry.getSenses().stream().map(VocabularySense::getExampleEn).collect(Collectors.toList());

        return PublicVocabularyDetailDto.builder()
                .id(entry.getId())
                .word(entry.getWord())
                .commonMeaningVi(entry.getCommonMeaningVn())
                .partOfSpeech(pos)
                .phonetic(phonetic)
                .cefrLevel(cefr)
                .topic(topic)
                .audioUsUrl(audioUsUrl)
                .audioUkUrl(audioUkUrl)
                
                .definitions(flatDefinitions)
                .examples(flatExamples)
                .synonyms(Collections.emptyList())
                .antonyms(Collections.emptyList())
                .collocations(Collections.emptyList())
                .idioms(Collections.emptyList())
                .phrasalVerbs(Collections.emptyList())
                
                .senses(senses)
                .structuredPhrasalVerbs(phrasalVerbs)
                .structuredIdioms(idioms)
                .isSaved(isSaved)
                .build();
    }

    public String getTopicIcon(String topicName) {
        if (topicName == null) return "book";
        return switch (topicName.toLowerCase()) {
            case "giáo dục & học tập" -> "graduation-cap";
            case "môi trường & bảo tồn" -> "leaf";
            case "khoa học & công nghệ" -> "cpu";
            case "kinh doanh & tài chính" -> "briefcase";
            case "công việc & nghề nghiệp" -> "user-check";
            case "sức khỏe & y tế" -> "heart-pulse";
            case "xã hội & luật pháp" -> "scale";
            case "văn hóa & nghệ thuật" -> "palette";
            case "du lịch & giao thông" -> "plane";
            case "con người, cảm xúc & tính cách" -> "smile";
            case "gia đình & đời sống cá nhân" -> "home";
            case "truyền thông & giải trí" -> "tv";
            case "thực phẩm & dinh dưỡng" -> "utensils";
            case "đô thị & nhà ở" -> "building";
            case "triết học, ngôn ngữ & tư duy" -> "brain";
            default -> "book-open";
        };
    }
}

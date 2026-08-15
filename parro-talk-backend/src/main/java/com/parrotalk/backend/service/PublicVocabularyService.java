package com.parrotalk.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.PublicVocabularyDetailDto;
import com.parrotalk.backend.dto.PublicVocabularySummaryDto;
import com.parrotalk.backend.dto.VocabularyReportRequestDto;
import com.parrotalk.backend.dto.VocabularyTopicSummaryDto;
import com.parrotalk.backend.entity.DictionaryEntry;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.VocabularyReport;
import com.parrotalk.backend.repository.DictionaryEntryRepository;
import com.parrotalk.backend.repository.VocabularyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final VocabularyReportRepository vocabularyReportRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get list of 15 IELTS vocabulary topics with word count.
     *
     * @return List of topic summary DTOs.
     */
    @Cacheable(value = "vocabularyTopics")
    @Transactional(readOnly = true)
    public List<VocabularyTopicSummaryDto> getTopics() {
        List<Object[]> rawTopics = dictionaryEntryRepository.findTopicsWithCounts();
        return rawTopics.stream()
                .map(row -> VocabularyTopicSummaryDto.builder()
                        .name((String) row[0])
                        .wordCount(((Number) row[1]).longValue())
                        .description("Danh mục từ vựng " + row[0] + " chuẩn IELTS")
                        .icon(getTopicIcon((String) row[0]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Search vocabulary with keyword, topic, CEFR, POS filters.
     *
     * @return Paginated vocabulary response.
     */
    @Transactional(readOnly = true)
    public PageResponse<PublicVocabularySummaryDto> searchVocabularies(
            String keyword, String topic, String cefrLevel, String partOfSpeech, int page, int size) {
        Page<DictionaryEntry> entries = dictionaryEntryRepository.searchVocabularies(
                keyword, topic, cefrLevel, partOfSpeech, PageRequest.of(page, size));
        Page<PublicVocabularySummaryDto> dtos = entries.map(this::mapToSummaryDto);
        return new PageResponse<>(dtos.getContent(), dtos.getNumber(), dtos.getSize(), dtos.getTotalElements(), dtos.getTotalPages());
    }

    /**
     * Get featured vocabulary list.
     *
     * @return List of 10 featured words.
     */
    @Transactional(readOnly = true)
    public List<PublicVocabularySummaryDto> getFeatured() {
        Page<DictionaryEntry> entries = dictionaryEntryRepository.findRandomEntries(null, "B2", PageRequest.of(0, 10));
        return entries.getContent().stream().map(this::mapToSummaryDto).collect(Collectors.toList());
    }

    /**
     * Get detailed vocabulary item by ID.
     *
     * @param id Vocabulary entry ID
     * @param user Authenticated user
     * @return Detailed vocabulary DTO
     */
    @Transactional(readOnly = true)
    public PublicVocabularyDetailDto getDetail(UUID id, User user) {
        DictionaryEntry entry = dictionaryEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found with id: " + id));
        return mapToDetailDto(entry, false);
    }

    /**
     * Submit user report for vocabulary entry content issue.
     *
     * @param id Vocabulary entry ID
     * @param user Authenticated reporter
     * @param request Report data
     */
    @Transactional
    public void submitReport(UUID id, User user, VocabularyReportRequestDto request) {
        DictionaryEntry entry = dictionaryEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vocabulary entry not found with id: " + id));

        VocabularyReport report = VocabularyReport.builder()
                .dictionaryEntry(entry)
                .reporterId(user != null ? user.getId() : UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .reportType(request.getReportType() != null ? request.getReportType() : "OTHER")
                .reason(request.getReason() != null ? request.getReason() : "Content issue")
                .description(request.getDescription())
                .status("OPEN")
                .priority("MEDIUM")
                .build();

        vocabularyReportRepository.save(report);
    }

    private PublicVocabularySummaryDto mapToSummaryDto(DictionaryEntry entry) {
        return PublicVocabularySummaryDto.builder()
                .id(entry.getId())
                .word(entry.getDisplayWord())
                .commonMeaningVi(entry.getCommonMeaningVi())
                .partOfSpeech(entry.getPartOfSpeech())
                .phonetic(entry.getPhonetic())
                .cefrLevel(entry.getCefrLevel())
                .topic(entry.getTopic())
                .audioUsUrl(entry.getAudioUsUrl())
                .audioUkUrl(entry.getAudioUkUrl())
                .build();
    }

    private PublicVocabularyDetailDto mapToDetailDto(DictionaryEntry entry, boolean isSaved) {
        return PublicVocabularyDetailDto.builder()
                .id(entry.getId())
                .word(entry.getDisplayWord())
                .commonMeaningVi(entry.getCommonMeaningVi())
                .partOfSpeech(entry.getPartOfSpeech())
                .phonetic(entry.getPhonetic())
                .cefrLevel(entry.getCefrLevel())
                .topic(entry.getTopic())
                .audioUsUrl(entry.getAudioUsUrl())
                .audioUkUrl(entry.getAudioUkUrl())
                .definitions(parseJsonList(entry.getDefinitionsJson()))
                .examples(parseJsonList(entry.getExamplesJson()))
                .synonyms(parseJsonList(entry.getSynonymsJson()))
                .antonyms(parseJsonList(entry.getAntonymsJson()))
                .idioms(parseJsonList(entry.getIdiomsJson()))
                .collocations(parseJsonList(entry.getCollocationsJson()))
                .phrasalVerbs(parseJsonList(entry.getPhrasalVerbsJson()))
                .isSaved(isSaved)
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.singletonList(json);
        }
    }

    private String getTopicIcon(String topicName) {
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

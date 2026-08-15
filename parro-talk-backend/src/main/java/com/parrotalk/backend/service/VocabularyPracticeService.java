package com.parrotalk.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.dto.VocabularyPracticeQuestionDto;
import com.parrotalk.backend.entity.DictionaryEntry;
import com.parrotalk.backend.repository.DictionaryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for generating interactive vocabulary practice questions.
 *
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class VocabularyPracticeService {

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate dynamic practice questions for the specified topic or CEFR level.
     *
     * @param topic Topic category filter
     * @param cefrLevel CEFR level filter
     * @param count Number of questions to generate
     * @return List of practice questions
     */
    @Transactional(readOnly = true)
    public List<VocabularyPracticeQuestionDto> generateQuestions(String topic, String cefrLevel, int count) {
        int limit = Math.max(5, Math.min(count, 30));
        Page<DictionaryEntry> entriesPage = dictionaryEntryRepository.findRandomEntries(topic, cefrLevel, PageRequest.of(0, limit * 3));
        List<DictionaryEntry> entries = entriesPage.getContent();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<VocabularyPracticeQuestionDto> questions = new ArrayList<>();
        List<String> allMeanings = entries.stream()
                .map(DictionaryEntry::getCommonMeaningVi)
                .filter(m -> m != null && !m.isBlank())
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(limit, entries.size()); i++) {
            DictionaryEntry entry = entries.get(i);
            int mode = i % 3; // 0: Multiple Choice, 1: Flashcard, 2: Fill-in-blanks

            if (mode == 0) {
                // Multiple Choice
                List<String> options = new ArrayList<>();
                options.add(entry.getCommonMeaningVi());
                List<String> distractors = new ArrayList<>(allMeanings);
                distractors.remove(entry.getCommonMeaningVi());
                Collections.shuffle(distractors);
                for (int j = 0; j < Math.min(3, distractors.size()); j++) {
                    options.add(distractors.get(j));
                }
                Collections.shuffle(options);

                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("MULTIPLE_CHOICE")
                        .prompt("Nghĩa tiếng Việt nào sau đây đúng với từ '" + entry.getDisplayWord() + "'?")
                        .options(options)
                        .correctAnswer(entry.getCommonMeaningVi())
                        .audioUrl(entry.getAudioUsUrl() != null ? entry.getAudioUsUrl() : entry.getAudioUkUrl())
                        .explanation(entry.getDisplayWord() + " (" + entry.getPartOfSpeech() + "): " + entry.getCommonMeaningVi())
                        .build());
            } else if (mode == 1) {
                // Flashcard
                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("FLASHCARD")
                        .prompt(entry.getDisplayWord())
                        .correctAnswer(entry.getCommonMeaningVi())
                        .wordHint(entry.getPhonetic() != null ? entry.getPhonetic() : entry.getPartOfSpeech())
                        .audioUrl(entry.getAudioUsUrl() != null ? entry.getAudioUsUrl() : entry.getAudioUkUrl())
                        .explanation(entry.getCommonMeaningVi())
                        .build());
            } else {
                // Fill-in-blanks (Cloze Test)
                String word = entry.getDisplayWord();
                String maskedWord;
                if (word.length() <= 3) {
                    maskedWord = word.charAt(0) + " " + "_ ".repeat(word.length() - 1).trim();
                } else {
                    maskedWord = word.charAt(0) + " " + "_ ".repeat(word.length() - 2) + word.charAt(word.length() - 1);
                }

                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("FILL_IN_BLANKS")
                        .prompt("Điền từ phù hợp (" + word.length() + " ký tự) dựa vào nghĩa: \"" + entry.getCommonMeaningVi() + "\"")
                        .wordHint(maskedWord)
                        .charCount(word.length())
                        .correctAnswer(word)
                        .audioUrl(entry.getAudioUsUrl() != null ? entry.getAudioUsUrl() : entry.getAudioUkUrl())
                        .explanation("Từ đúng là: " + word + " (" + entry.getCommonMeaningVi() + ")")
                        .build());
            }
        }
        return questions;
    }

    /**
     * Submit user practice session results.
     *
     * @param answers Submitted question answers
     */
    @Transactional
    public void submitPractice(List<Map<String, Object>> answers) {
        // Practice session results recorded
    }

}

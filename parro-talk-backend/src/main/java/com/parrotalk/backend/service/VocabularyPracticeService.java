package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.VocabularyPracticeQuestionDto;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
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

    private final VocabularyRepository vocabularyRepository;
    private final AiCoreClient aiCoreClient;

    @Transactional(readOnly = true)
    public List<VocabularyPracticeQuestionDto> generateQuestions(String topic, String cefrLevel, int count) {
        int limit = Math.max(5, Math.min(count, 50));
        List<Vocabulary> entries = vocabularyRepository.findRandomVocabularies(topic, cefrLevel, limit * 2);
        
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<VocabularyPracticeQuestionDto> questions = new ArrayList<>();
        List<String> allMeanings = entries.stream()
                .map(Vocabulary::getCommonMeaningVn)
                .filter(m -> m != null && !m.isBlank())
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(limit, entries.size()); i++) {
            Vocabulary entry = entries.get(i);
            int mode = i % 5; 
            
            VocabularySense sense = entry.getSenses().isEmpty() ? null : entry.getSenses().get(0);
            String definitionEn = sense != null && sense.getDefinitionEn() != null ? sense.getDefinitionEn() : entry.getCommonMeaningVn();
            String exampleEn = sense != null && sense.getExampleEn() != null ? sense.getExampleEn() : "";
            
            String audioUrl = entry.getPronunciations().isEmpty() ? null : entry.getPronunciations().get(0).getAudioUrl();

            if (mode == 0) { // Multiple Choice
                List<String> options = new ArrayList<>();
                options.add(entry.getCommonMeaningVn());
                List<String> distractors = new ArrayList<>(allMeanings);
                distractors.remove(entry.getCommonMeaningVn());
                Collections.shuffle(distractors);
                for (int j = 0; j < Math.min(3, distractors.size()); j++) {
                    options.add(distractors.get(j));
                }
                Collections.shuffle(options);

                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("MULTIPLE_CHOICE")
                        .prompt("Nghĩa tiếng Việt nào sau đây đúng với từ '" + entry.getWord() + "'?")
                        .options(options)
                        .correctAnswer(entry.getCommonMeaningVn())
                        .audioUrl(audioUrl)
                        .explanation(entry.getWord() + ": " + entry.getCommonMeaningVn())
                        .build());
            } else if (mode == 1) { // Flashcard
                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("FLASHCARD")
                        .prompt(entry.getWord())
                        .correctAnswer(entry.getCommonMeaningVn())
                        .audioUrl(audioUrl)
                        .explanation(entry.getCommonMeaningVn())
                        .build());
            } else if (mode == 2) { // Fill in the blanks
                String word = entry.getWord();
                String sentenceWithBlank = exampleEn.replaceAll("(?i)\\b" + word + "\\b", "______");
                if (sentenceWithBlank.equals(exampleEn)) {
                    sentenceWithBlank = "The correct answer is ______.";
                }
                
                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("FILL_IN_BLANKS")
                        .prompt("Điền từ vào chỗ trống")
                        .correctAnswer(word)
                        .sentenceWithBlank(sentenceWithBlank)
                        .audioUrl(audioUrl)
                        .explanation(exampleEn)
                        .build());
            } else if (mode == 3) { // Type the Word
                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("TYPE_WORD")
                        .prompt("Type the word")
                        .correctAnswer(entry.getWord())
                        .definition(definitionEn)
                        .firstLetter(entry.getWord().substring(0, 1))
                        .charCount(entry.getWord().length())
                        .exampleSentence(exampleEn)
                        .audioUrl(audioUrl)
                        .explanation(definitionEn)
                        .build());
            } else if (mode == 4) { // Listen & Type
                String word = entry.getWord();
                String sentenceWithBlank = exampleEn.replaceAll("(?i)\\b" + word + "\\b", "______");
                if (sentenceWithBlank.equals(exampleEn)) {
                    sentenceWithBlank = "The correct answer is ______.";
                }

                // AI generate clues for hard mode. Fallback if it fails.
                List<String> clues = List.of(
                        "It is related to: " + (sense != null && sense.getTopic() != null ? sense.getTopic().getName() : "General"),
                        "Definition: " + definitionEn,
                        "Meaning in Vietnamese: " + entry.getCommonMeaningVn()
                );
                
                try {
                    String prompt = "Generate 3 short clues to guess the word '" + word + "'. Definition: " + definitionEn + ". Output exactly 3 lines, each a clue. Do not include the word itself.";
                    String aiResponse = aiCoreClient.callAndValidate("You are a helpful English teacher.", prompt, String.class, java.util.function.Function.identity());
                    if (aiResponse != null && !aiResponse.isBlank()) {
                        clues = java.util.Arrays.asList(aiResponse.split("\n"));
                    }
                } catch (Exception ignored) {}

                questions.add(VocabularyPracticeQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .wordId(entry.getId())
                        .questionType("LISTEN_TYPE")
                        .prompt("Listen and type")
                        .correctAnswer(word)
                        .definition(definitionEn)
                        .sentenceWithBlank(sentenceWithBlank)
                        .easySentence(exampleEn)
                        .fullSentence(exampleEn)
                        .clues(clues)
                        .audioUrl(audioUrl)
                        .explanation(exampleEn)
                        .build());
            }
        }
        return questions;
    }

    public void submitPractice(List<Map<String, Object>> answers) {
        // Handle logic for submission
    }
}

package com.parrotalk.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.TopicPracticeSentence;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.TopicPracticeSentenceRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for generating IELTS topic-contextual practice sentences with blanks,
 * grammar tips, and explanations using Spring AI (AiCoreClient).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TopicPracticeSentenceAiService {

    private final AiCoreClient aiCoreClient;
    private final TopicPracticeSentenceRepository sentenceRepository;

    private static final String SYSTEM_PROMPT = """
            You are an expert English linguist and IELTS examiner creating fill-in-the-blank practice sentences.
            For the given vocabulary word and topic, construct a natural, context-rich English sentence related to the topic.
            Requirements:
            1. The sentence must clearly demonstrate the target word in context of the given topic.
            2. The target word in the sentence may be the base form or a grammatically appropriate inflected form (e.g. past tense, plural noun, gerund).
            3. The blank in sentenceWithBlank must be formatted as exactly "______" (6 underscores) replacing the targetForm.
            4. completedSentence is the full sentence with targetForm filled in.
            5. sentenceVn is a natural, fluent Vietnamese translation of the completed sentence.
            6. grammarContextTip: A short 1-sentence tip on grammar structure (e.g. "Follows infinitive marker 'to' -> requires base form verb", "Requires plural noun after 'several financial'"). NEVER include targetWord or targetForm in grammarContextTip.
            7. explanation: A 1-2 sentence explanation clarifying the grammatical reason and topic meaning.
            8. Output ONLY a valid JSON object matching the schema, with no markdown code fences or comments.
            """;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiSentenceResponse {
        @JsonProperty("targetWord")
        private String targetWord;

        @JsonProperty("targetForm")
        private String targetForm;

        @JsonProperty("sentenceWithBlank")
        private String sentenceWithBlank;

        @JsonProperty("completedSentence")
        private String completedSentence;

        @JsonProperty("sentenceVn")
        private String sentenceVn;

        @JsonProperty("grammarContextTip")
        private String grammarContextTip;

        @JsonProperty("explanation")
        private String explanation;
    }

    /**
     * Generates a topic-contextual sentence using AI, with fallback to dictionary example if AI is unreachable.
     */
    public TopicPracticeSentence generateSentence(Vocabulary vocabulary, Topic topic, VocabularySense sense) {
        String word = vocabulary.getWord();
        String topicName = topic != null ? topic.getName() : "General";
        String pos = (sense != null && sense.getPartOfSpeech() != null) ? sense.getPartOfSpeech() : "vocabulary word";
        String defEn = (sense != null && sense.getDefinitionEn() != null) ? sense.getDefinitionEn() : word;

        String userPrompt = String.format("""
                Topic: %s
                Target Word: %s
                Part of Speech: %s
                Definition: %s
                
                Generate a fill-in-the-blank sentence with blank "______", completed sentence, Vietnamese translation, grammar tip, and explanation in JSON format.
                """, topicName, word, pos, defEn);

        try {
            log.info("Prompting AI for contextual sentence: word='{}', topic='{}'", word, topicName);
            AiSentenceResponse response = aiCoreClient.callAndValidate(
                    SYSTEM_PROMPT,
                    userPrompt,
                    AiSentenceResponse.class,
                    raw -> {
                        if (raw == null) {
                            throw new IllegalArgumentException("AI returned null sentence response");
                        }
                        if (raw.getSentenceWithBlank() == null || !raw.getSentenceWithBlank().contains("______")) {
                            throw new IllegalArgumentException("AI sentence does not contain '______' blank marker");
                        }
                        if (raw.getTargetForm() == null || raw.getTargetForm().isBlank()) {
                            raw.setTargetForm(word);
                        }
                        if (raw.getCompletedSentence() == null || raw.getCompletedSentence().isBlank()) {
                            raw.setCompletedSentence(raw.getSentenceWithBlank().replace("______", raw.getTargetForm()));
                        }
                        if (raw.getGrammarContextTip() != null && !raw.getGrammarContextTip().isBlank()) {
                            raw.setGrammarContextTip(sanitizeGrammarTip(raw.getGrammarContextTip(), word, raw.getTargetForm()));
                        }
                        return raw;
                    }
            );

            return TopicPracticeSentence.builder()
                    .vocabulary(vocabulary)
                    .topic(topic)
                    .targetWord(word)
                    .targetForm(response.getTargetForm() != null ? response.getTargetForm().trim() : word)
                    .sentenceWithBlank(response.getSentenceWithBlank().trim())
                    .completedSentence(response.getCompletedSentence().trim())
                    .sentenceVn(response.getSentenceVn() != null ? response.getSentenceVn().trim() : "")
                    .grammarContextTip(response.getGrammarContextTip() != null ? response.getGrammarContextTip().trim() : "")
                    .explanation(response.getExplanation() != null ? response.getExplanation().trim() : "")
                    .build();

        } catch (Exception ex) {
            log.warn("AI generation failed for word '{}' and topic '{}'. Gracefully falling back to dictionary example: {}",
                    word, topicName, ex.getMessage());
            return buildFallbackSentence(vocabulary, topic, sense);
        }
    }

    /**
     * Generates and persists sentence to repository.
     */
    public TopicPracticeSentence generateAndPersistSentence(Vocabulary vocabulary, Topic topic, VocabularySense sense) {
        TopicPracticeSentence sentence = generateSentence(vocabulary, topic, sense);
        return sentenceRepository.save(sentence);
    }

    /**
     * Constructs a graceful fallback practice sentence from the dictionary sense example.
     */
    public TopicPracticeSentence buildFallbackSentence(Vocabulary vocabulary, Topic topic, VocabularySense sense) {
        String word = vocabulary.getWord();
        String topicName = topic != null ? topic.getName() : "General";
        String exampleEn = (sense != null && sense.getExampleEn() != null && !sense.getExampleEn().isBlank())
                ? sense.getExampleEn().trim()
                : null;
        String exampleVn = (sense != null && sense.getExampleVn() != null && !sense.getExampleVn().isBlank())
                ? sense.getExampleVn().trim()
                : (sense != null && sense.getDefinitionVn() != null ? sense.getDefinitionVn().trim() : "Ví dụ thực hành từ vựng theo ngữ cảnh.");

        String pos = (sense != null && sense.getPartOfSpeech() != null) ? sense.getPartOfSpeech() : "word";
        String defEn = (sense != null && sense.getDefinitionEn() != null) ? sense.getDefinitionEn() : word;

        String sentenceWithBlank;
        String completedSentence;
        String targetForm = word;

        Matcher exactOrInflectedMatcher = (exampleEn != null && !exampleEn.isBlank())
                ? buildWordOrInflectionPattern(word).matcher(exampleEn)
                : null;

        if (exactOrInflectedMatcher != null && exactOrInflectedMatcher.find()) {
            targetForm = exactOrInflectedMatcher.group(1);
            completedSentence = exampleEn;
            sentenceWithBlank = exactOrInflectedMatcher.replaceFirst("______");
        } else {
            completedSentence = "In the topic of " + topicName + ", the term " + word + " plays an important role.";
            sentenceWithBlank = "In the topic of " + topicName + ", the term ______ plays an important role.";
        }

        String tip = "Identify the correct form and part of speech (" + pos + ") that fits this sentence context.";
        String explanation = "'" + word + "' (" + pos + "): " + defEn + ". Fits contextually within the " + topicName + " theme.";

        return TopicPracticeSentence.builder()
                .vocabulary(vocabulary)
                .topic(topic)
                .targetWord(word)
                .targetForm(targetForm)
                .sentenceWithBlank(sentenceWithBlank)
                .completedSentence(completedSentence)
                .sentenceVn(exampleVn)
                .grammarContextTip(tip)
                .explanation(explanation)
                .fallback(true)
                .build();
    }

    private Pattern buildWordOrInflectionPattern(String word) {
        String cleanWord = word == null ? "" : word.trim();
        if (cleanWord.length() >= 4 && (cleanWord.endsWith("e") || cleanWord.endsWith("y") || cleanWord.endsWith("E") || cleanWord.endsWith("Y"))) {
            String stem = cleanWord.substring(0, cleanWord.length() - 1);
            String regex = "(?i)\\b(" + Pattern.quote(cleanWord) + "[a-z]*|" + Pattern.quote(stem) + "(?:s|es|ed|ing|er|ers|ier|iest|ies|ation|ations))\\b";
            return Pattern.compile(regex);
        }
        return Pattern.compile("(?i)\\b(" + Pattern.quote(cleanWord) + "[a-z]*)\\b");
    }

    private String sanitizeGrammarTip(String tip, String targetWord, String targetForm) {
        if (tip == null || tip.isBlank()) {
            return "";
        }
        String sanitized = tip;
        if (targetForm != null && !targetForm.isBlank()) {
            sanitized = sanitized.replaceAll("(?i)['\"“”‘’]?\\b" + Pattern.quote(targetForm.trim()) + "\\b['\"“”‘’]?", "");
        }
        if (targetWord != null && !targetWord.isBlank()) {
            sanitized = sanitized.replaceAll("(?i)['\"“”‘’]?\\b" + Pattern.quote(targetWord.trim()) + "\\b['\"“”‘’]?", "");
        }
        return sanitized.replaceAll("\\s{2,}", " ").replaceAll("\\s+([.,;:])", "$1").trim();
    }
}

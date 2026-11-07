package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.VocabularyStatus;
import com.parrotalk.backend.dto.VocabularyPracticeQuestionDto;
import com.parrotalk.backend.dto.practice.FillBlankQuestionDto;
import com.parrotalk.backend.dto.practice.LearnWordDto;
import com.parrotalk.backend.dto.practice.MultipleChoiceQuestionDto;
import com.parrotalk.backend.dto.practice.TypeWordQuestionDto;
import com.parrotalk.backend.entity.SenseAntonym;
import com.parrotalk.backend.entity.SenseCollocation;
import com.parrotalk.backend.entity.SenseSynonym;
import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.TopicPracticeSentence;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserVocabulary;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.TopicPracticeSentenceRepository;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.UserVocabularyRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for generating interactive vocabulary practice questions and mode-specific suites:
 * 1. Learn Words (interactive 3D flashcards with reversible translations)
 * 2. Multiple Choice Quiz
 * 3. Type the Word
 * 4. Fill in the Blank (Hybrid AI: DB Cache -> On-Demand AI Fallback -> DB Save)
 *
 * @author MinhTuMTN
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VocabularyPracticeService {

    private static final int MAX_SYNCHRONOUS_AI_CALLS_PER_REQUEST = 1;

    private volatile long aiCooldownUntilEpochMs = 0L;

    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final TopicPracticeSentenceRepository sentenceRepository;
    private final TopicPracticeSentenceAiService aiService;
    private final AiCoreClient aiCoreClient;
    private final UserVocabularyRepository userVocabularyRepository;

    private List<Vocabulary> fetchRandomVocabulariesWithExclusions(
            String topicName,
            String cefrLevel,
            int limit,
            List<String> excludeWords,
            User user) {
        List<String> combined = new ArrayList<>();
        if (excludeWords != null) {
            for (String w : excludeWords) {
                if (w != null && !w.trim().isEmpty()) {
                    combined.add(w.trim().toLowerCase());
                }
            }
        }
        if (user != null && user.getId() != null && userVocabularyRepository != null && topicName != null && !topicName.isBlank()) {
            try {
                List<String> learned = userVocabularyRepository.findLearnedNormalizedWordsByUserIdAndTopicName(user.getId(), topicName.trim());
                if (learned != null) {
                    for (String w : learned) {
                        if (w != null && !w.trim().isEmpty()) {
                            combined.add(w.trim().toLowerCase());
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to load learned words for user {} in topic {}: {}", user.getId(), topicName, ex.getMessage());
            }
        }
        if (combined.isEmpty()) {
            return vocabularyRepository.findRandomVocabularies(topicName, cefrLevel, limit);
        }
        return vocabularyRepository.findRandomVocabularies(topicName, cefrLevel, limit, combined.stream().distinct().toList());
    }

    /**
     * R1 Hybrid Flow: Fill in the Blank practice questions.
     * 1. Query database first via TopicPracticeSentenceRepository.
     * 2. If absent, generate on-demand via TopicPracticeSentenceAiService and persist to DB (only when AI succeeds).
     * 3. Fallback to dictionary example if AI fails or circuit breaker trips.
     */
    @Transactional
    public List<FillBlankQuestionDto> getFillBlankQuestions(String topicName, int limit) {
        return getFillBlankQuestions(topicName, limit, null, null);
    }

    @Transactional
    public List<FillBlankQuestionDto> getFillBlankQuestions(String topicName, int limit, List<String> excludeWords, User user) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        Topic topic = resolveTopic(topicName);
        List<Vocabulary> entries = fetchRandomVocabulariesWithExclusions(topicName, null, boundedLimit * 2, excludeWords, user);

        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        int targetCount = Math.min(boundedLimit, entries.size());
        Map<UUID, TopicPracticeSentence> batchCacheByVocabId = new java.util.HashMap<>();
        boolean useBatchLookup = topic != null && topic.getId() != null && targetCount > 2;
        if (useBatchLookup) {
            List<UUID> vocabIds = entries.subList(0, targetCount).stream()
                    .map(Vocabulary::getId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (!vocabIds.isEmpty()) {
                List<TopicPracticeSentence> batchCached = sentenceRepository.findByTopicIdAndVocabularyIdIn(topic.getId(), vocabIds);
                if (batchCached != null) {
                    for (TopicPracticeSentence s : batchCached) {
                        if (s != null && s.getVocabulary() != null && s.getVocabulary().getId() != null) {
                            batchCacheByVocabId.putIfAbsent(s.getVocabulary().getId(), s);
                        }
                    }
                }
            }
        }

        List<FillBlankQuestionDto> results = new ArrayList<>();
        boolean aiUnavailable = targetCount > 2 && System.currentTimeMillis() < aiCooldownUntilEpochMs;
        int aiCallsCount = 0;

        for (int i = 0; i < targetCount; i++) {
            Vocabulary vocab = entries.get(i);
            VocabularySense sense = resolveSenseForTopic(vocab, topic, topicName);
            String word = vocab.getWord();

            Optional<TopicPracticeSentence> cachedOpt = Optional.empty();
            if (useBatchLookup) {
                if (vocab.getId() != null) {
                    cachedOpt = Optional.ofNullable(batchCacheByVocabId.get(vocab.getId()));
                }
            } else {
                if (topic != null && topic.getId() != null) {
                    cachedOpt = sentenceRepository.findFirstByTopicIdAndVocabularyId(topic.getId(), vocab.getId());
                    if (cachedOpt.isEmpty()) {
                        cachedOpt = sentenceRepository.findFirstByTopicIdAndTargetWord(topic.getId(), word);
                    }
                }
                if (cachedOpt.isEmpty() && vocab.getId() != null) {
                    cachedOpt = sentenceRepository.findFirstByVocabularyId(vocab.getId());
                }
                if (cachedOpt.isEmpty()) {
                    cachedOpt = sentenceRepository.findFirstByTargetWord(word);
                }
            }

            TopicPracticeSentence sentence;
            String source;

            if (cachedOpt.isPresent()) {
                log.info("Serving pre-generated sentence from DB for word: {}", word);
                sentence = cachedOpt.get();
                source = "DATABASE";
            } else if (aiUnavailable || aiCallsCount >= MAX_SYNCHRONOUS_AI_CALLS_PER_REQUEST) {
                log.info("Skipping synchronous AI generation for word '{}' (aiUnavailable={}, aiCallsCount={}), using dictionary fallback",
                        word, aiUnavailable, aiCallsCount);
                sentence = aiService.buildFallbackSentence(vocab, topic, sense);
                source = "DICTIONARY_FALLBACK";
            } else {
                log.info("No pre-generated sentence found in DB. Generating on-demand via AI for word: {}", word);
                aiCallsCount++;
                boolean fellBackOnException = false;
                try {
                    sentence = aiService.generateSentence(vocab, topic, sense);
                } catch (Exception ex) {
                    log.warn("AI generation failed for word '{}', using dictionary fallback: {}", word, ex.getMessage());
                    sentence = aiService.buildFallbackSentence(vocab, topic, sense);
                    fellBackOnException = true;
                    aiUnavailable = true;
                }

                if (sentence == null) {
                    sentence = aiService.buildFallbackSentence(vocab, topic, sense);
                    fellBackOnException = true;
                    aiUnavailable = true;
                } else if (sentence.isFallback()) {
                    aiUnavailable = true;
                }

                if (aiUnavailable && targetCount > 2) {
                    aiCooldownUntilEpochMs = System.currentTimeMillis() + 60_000L;
                }

                if (!fellBackOnException && !sentence.isFallback()) {
                    try {
                        sentence = sentenceRepository.save(sentence);
                    } catch (Exception saveEx) {
                        log.warn("Failed to persist generated practice sentence for {}: {}", word, saveEx.getMessage());
                    }
                    source = "AI_GENERATED";
                } else {
                    source = "DICTIONARY_FALLBACK";
                }
            }

            String audioUrl = extractAudioUrl(vocab);
            String partOfSpeech = sense != null ? sense.getPartOfSpeech() : null;
            String cefrLevel = sense != null ? sense.getCefrLevel() : null;
            String definitionEn = sense != null && sense.getDefinitionEn() != null && !sense.getDefinitionEn().isBlank()
                    ? sense.getDefinitionEn()
                    : null;
            String definitionVn = sense != null && sense.getDefinitionVn() != null && !sense.getDefinitionVn().isBlank()
                    ? sense.getDefinitionVn()
                    : vocab.getCommonMeaningVn();
            List<String> variants = new ArrayList<>();
            if (sentence.getTargetForm() != null && !sentence.getTargetForm().isBlank()) {
                variants.add(sentence.getTargetForm());
            }
            if (!variants.contains(word)) {
                variants.add(word);
            }

            results.add(FillBlankQuestionDto.builder()
                    .id(sentence.getId() != null ? sentence.getId() : UUID.randomUUID())
                    .word(word)
                    .targetForm(sentence.getTargetForm() != null ? sentence.getTargetForm() : word)
                    .sentenceWithBlank(sentence.getSentenceWithBlank())
                    .completedSentence(sentence.getCompletedSentence())
                    .sentenceVn(sentence.getSentenceVn())
                    .grammarContextTip(sentence.getGrammarContextTip())
                    .explanation(sentence.getExplanation())
                    .source(source)
                    .partOfSpeech(partOfSpeech)
                    .cefrLevel(cefrLevel)
                    .definitionEn(definitionEn)
                    .definitionVn(definitionVn)
                    .audioUrl(audioUrl)
                    .acceptableVariants(variants)
                    .build());
        }

        return results;
    }

    /**
     * Mode 1: Learn Words (Flashcards) with reversible EN ↔ VI translations and rich dictionary senses.
     */
    @Transactional(readOnly = true)
    public List<LearnWordDto> getLearnWordCards(String topicName, int limit) {
        return getLearnWordCards(topicName, limit, null, null);
    }

    @Transactional(readOnly = true)
    public List<LearnWordDto> getLearnWordCards(String topicName, int limit, List<String> excludeWords, User user) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        List<Vocabulary> entries = fetchRandomVocabulariesWithExclusions(topicName, null, boundedLimit, excludeWords, user);

        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<LearnWordDto> cards = new ArrayList<>();
        for (Vocabulary vocab : entries) {
            VocabularySense sense = resolveSenseForTopic(vocab, null, topicName);
            PronunciationPair audio = extractPronunciations(vocab);

            List<String> synonyms = sense != null && sense.getSynonyms() != null
                    ? sense.getSynonyms().stream().map(SenseSynonym::getSynonymWord).filter(s -> s != null && !s.isBlank()).toList()
                    : Collections.emptyList();

            List<String> antonyms = sense != null && sense.getAntonyms() != null
                    ? sense.getAntonyms().stream().map(SenseAntonym::getAntonymWord).filter(s -> s != null && !s.isBlank()).toList()
                    : Collections.emptyList();

            List<String> collocations = sense != null && sense.getCollocations() != null
                    ? sense.getCollocations().stream().map(SenseCollocation::getCollocationWord).filter(s -> s != null && !s.isBlank()).toList()
                    : Collections.emptyList();

            String defEn = sense != null && sense.getDefinitionEn() != null ? sense.getDefinitionEn() : vocab.getWord();
            String defVn = sense != null && sense.getDefinitionVn() != null ? sense.getDefinitionVn() : vocab.getCommonMeaningVn();

            cards.add(LearnWordDto.builder()
                    .id(vocab.getId() != null ? vocab.getId() : UUID.randomUUID())
                    .word(vocab.getWord())
                    .phonetic(audio.generalIpa())
                    .phoneticUs(audio.usIpa())
                    .phoneticUk(audio.ukIpa())
                    .audioUrl(audio.generalAudio())
                    .audioUrlUs(audio.usAudio())
                    .audioUrlUk(audio.ukAudio())
                    .partOfSpeech(sense != null ? sense.getPartOfSpeech() : null)
                    .cefrLevel(sense != null ? sense.getCefrLevel() : null)
                    .topic(topicName)
                    .definitionEn(defEn)
                    .definitionVn(defVn)
                    .exampleEn(sense != null ? sense.getExampleEn() : null)
                    .exampleVn(sense != null ? sense.getExampleVn() : null)
                    .synonyms(synonyms)
                    .antonyms(antonyms)
                    .collocations(collocations)
                    .build());
        }

        return cards;
    }

    /**
     * Mode 2: Multiple Choice Quiz with topic-relevant distractors and randomized options.
     */
    @Transactional(readOnly = true)
    public List<MultipleChoiceQuestionDto> getMultipleChoiceQuestions(String topicName, int limit) {
        return getMultipleChoiceQuestions(topicName, limit, null, null);
    }

    @Transactional(readOnly = true)
    public List<MultipleChoiceQuestionDto> getMultipleChoiceQuestions(String topicName, int limit, List<String> excludeWords, User user) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        List<Vocabulary> entries = fetchRandomVocabulariesWithExclusions(topicName, null, boundedLimit * 2, excludeWords, user);

        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> allDefinitions = entries.stream()
                .map(v -> {
                    VocabularySense s = resolveSenseForTopic(v, null, topicName);
                    if (s != null && s.getDefinitionEn() != null && !s.getDefinitionEn().isBlank()) {
                        return s.getDefinitionEn().trim();
                    }
                    return v.getCommonMeaningVn();
                })
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .collect(Collectors.toList());

        List<MultipleChoiceQuestionDto> questions = new ArrayList<>();
        for (int i = 0; i < Math.min(boundedLimit, entries.size()); i++) {
            Vocabulary vocab = entries.get(i);
            VocabularySense sense = resolveSenseForTopic(vocab, null, topicName);
            PronunciationPair audio = extractPronunciations(vocab);

            String correctAnswer = (sense != null && sense.getDefinitionEn() != null && !sense.getDefinitionEn().isBlank())
                    ? sense.getDefinitionEn().trim()
                    : (vocab.getCommonMeaningVn() != null ? vocab.getCommonMeaningVn() : vocab.getWord());

            List<String> distractors = new ArrayList<>(allDefinitions);
            distractors.remove(correctAnswer);
            Collections.shuffle(distractors);

            List<String> options = new ArrayList<>();
            options.add(correctAnswer);
            for (int j = 0; j < Math.min(3, distractors.size()); j++) {
                options.add(distractors.get(j));
            }
            while (options.size() < 4) {
                options.add("A related concept in " + (topicName != null ? topicName : "English") + " (Option " + (options.size() + 1) + ")");
            }
            Collections.shuffle(options);

            int correctIndex = options.indexOf(correctAnswer);
            String exampleSentence = sense != null ? sense.getExampleEn() : null;

            questions.add(MultipleChoiceQuestionDto.builder()
                    .id(vocab.getId() != null ? vocab.getId() : UUID.randomUUID())
                    .question("What is the meaning of '" + vocab.getWord() + "'?")
                    .targetWord(vocab.getWord())
                    .options(options)
                    .correctAnswer(correctAnswer)
                    .correctIndex(correctIndex)
                    .phonetic(audio.generalIpa())
                    .partOfSpeech(sense != null ? sense.getPartOfSpeech() : null)
                    .sentence(exampleSentence)
                    .explanation(vocab.getWord() + ": " + correctAnswer)
                    .audioUrl(audio.generalAudio())
                    .build());
        }

        return questions;
    }

    /**
     * Mode 3: Type the Word (active recall with definitions, character count, and hints).
     */
    @Transactional(readOnly = true)
    public List<TypeWordQuestionDto> getTypeWordQuestions(String topicName, int limit) {
        return getTypeWordQuestions(topicName, limit, null, null);
    }

    @Transactional(readOnly = true)
    public List<TypeWordQuestionDto> getTypeWordQuestions(String topicName, int limit, List<String> excludeWords, User user) {
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        List<Vocabulary> entries = fetchRandomVocabulariesWithExclusions(topicName, null, boundedLimit, excludeWords, user);

        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<TypeWordQuestionDto> questions = new ArrayList<>();
        for (Vocabulary vocab : entries) {
            VocabularySense sense = resolveSenseForTopic(vocab, null, topicName);
            PronunciationPair audio = extractPronunciations(vocab);
            String word = vocab.getWord();

            String defEn = sense != null && sense.getDefinitionEn() != null ? sense.getDefinitionEn() : word;
            String defVn = sense != null && sense.getDefinitionVn() != null ? sense.getDefinitionVn() : vocab.getCommonMeaningVn();

            String promptDef = defEn;
            if (defVn != null && !defVn.isBlank()) {
                promptDef = defEn + " (" + defVn + ")";
            }

            String exampleSentence = sense != null ? sense.getExampleEn() : null;
            String exampleWithMask = null;
            if (exampleSentence != null && !exampleSentence.isBlank()) {
                exampleWithMask = exampleSentence.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "________");
            }

            questions.add(TypeWordQuestionDto.builder()
                    .id(vocab.getId() != null ? vocab.getId() : UUID.randomUUID())
                    .targetWord(word)
                    .promptDefinition(promptDef)
                    .definitionEn(defEn)
                    .definitionVn(defVn)
                    .firstLetter(word.substring(0, 1).toLowerCase())
                    .characterCount(word.length())
                    .partOfSpeech(sense != null ? sense.getPartOfSpeech() : null)
                    .phonetic(audio.generalIpa())
                    .audioUrl(audio.generalAudio())
                    .exampleWithMask(exampleWithMask)
                    .exampleSentence(exampleSentence)
                    .build());
        }

        return questions;
    }

    /**
     * Backward-compatible practice questions generator with optional mode routing.
     */
    @Transactional
    public List<VocabularyPracticeQuestionDto> generateQuestions(String topic, String cefrLevel, int count, String mode) {
        if (mode != null && !mode.isBlank()) {
            String normalizedMode = mode.trim().toLowerCase();
            if ("fill-blank".equals(normalizedMode) || "cloze".equals(normalizedMode)) {
                return getFillBlankQuestions(topic, count).stream()
                        .map(fb -> VocabularyPracticeQuestionDto.builder()
                                .id(fb.getId())
                                .questionType("FILL_IN_BLANKS")
                                .prompt("Điền từ vào chỗ trống")
                                .correctAnswer(fb.getTargetForm())
                                .sentenceWithBlank(fb.getSentenceWithBlank())
                                .fullSentence(fb.getCompletedSentence())
                                .explanation(fb.getExplanation())
                                .audioUrl(fb.getAudioUrl())
                                .build())
                        .collect(Collectors.toList());
            } else if ("multiple-choice".equals(normalizedMode)) {
                return getMultipleChoiceQuestions(topic, count).stream()
                        .map(mc -> VocabularyPracticeQuestionDto.builder()
                                .id(mc.getId())
                                .questionType("MULTIPLE_CHOICE")
                                .prompt(mc.getQuestion())
                                .options(mc.getOptions())
                                .correctAnswer(mc.getCorrectAnswer())
                                .explanation(mc.getExplanation())
                                .audioUrl(mc.getAudioUrl())
                                .build())
                        .collect(Collectors.toList());
            } else if ("type-word".equals(normalizedMode)) {
                return getTypeWordQuestions(topic, count).stream()
                        .map(tw -> VocabularyPracticeQuestionDto.builder()
                                .id(tw.getId())
                                .questionType("TYPE_WORD")
                                .prompt("Type the word")
                                .correctAnswer(tw.getTargetWord())
                                .definition(tw.getPromptDefinition())
                                .firstLetter(tw.getFirstLetter())
                                .charCount(tw.getCharacterCount())
                                .exampleSentence(tw.getExampleSentence())
                                .audioUrl(tw.getAudioUrl())
                                .explanation(tw.getDefinitionEn())
                                .build())
                        .collect(Collectors.toList());
            }
        }

        return generateQuestions(topic, cefrLevel, count);
    }

    /**
     * Backward-compatible signature: generateQuestions(topic, cefrLevel, count).
     */
    @Transactional
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
            String audioUrl = extractAudioUrl(entry);

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
                String sentenceWithBlank = exampleEn.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "______");
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
                String sentenceWithBlank = exampleEn.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "______");
                if (sentenceWithBlank.equals(exampleEn)) {
                    sentenceWithBlank = "The correct answer is ______.";
                }

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

    /**
     * Backward-compatible convenience method: getPracticeQuestions(topicName, count).
     */
    @Transactional
    public List<VocabularyPracticeQuestionDto> getPracticeQuestions(String topicName, int count) {
        return generateQuestions(topicName, null, count, null);
    }

    @Transactional
    public void submitPractice(List<Map<String, Object>> answers) {
        submitPractice(answers, null);
    }

    @Transactional
    public void submitPractice(List<Map<String, Object>> answers, User user) {
        if (answers == null || answers.isEmpty() || user == null || user.getId() == null || userVocabularyRepository == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> item : answers) {
            if (item == null) {
                continue;
            }
            Object rawWord = item.get("word");
            if (rawWord == null) {
                rawWord = item.get("targetWord");
            }
            if (rawWord == null) {
                continue;
            }
            String displayWord = String.valueOf(rawWord).trim();
            if (displayWord.isEmpty()) {
                continue;
            }
            String normalizedWord = displayWord.toLowerCase();
            String rating = item.get("rating") != null ? String.valueOf(item.get("rating")).trim().toLowerCase() : null;
            String requestedStatus = item.get("status") != null ? String.valueOf(item.get("status")).trim().toUpperCase() : null;
            Object isCorrectObj = item.get("isCorrect");
            boolean isPositive = isCorrectObj instanceof Boolean b ? b
                    : (rating == null || !"hard".equals(rating));

            Optional<UserVocabulary> existingOpt = userVocabularyRepository.findByUserIdAndNormalizedWord(user.getId(), normalizedWord);
            UserVocabulary uv;
            if (existingOpt.isPresent()) {
                uv = existingOpt.get();
                uv.setReviewCount(uv.getReviewCount() + 1);
                uv.setLastReviewedAt(now);
                if (isPositive) {
                    int nextCorrect = (uv.getCorrectCount() != null ? uv.getCorrectCount() : 0) + 1;
                    uv.setCorrectCount(nextCorrect);
                    if ("easy".equals(rating) || "MASTERED".equals(requestedStatus)
                            || (uv.getStatus() == VocabularyStatus.LEARNING && nextCorrect >= 2)) {
                        uv.setStatus(VocabularyStatus.MASTERED);
                        uv.setNextReviewAt(now.plusDays(7));
                    } else if (uv.getStatus() == null || uv.getStatus() == VocabularyStatus.NEW) {
                        uv.setStatus(VocabularyStatus.LEARNING);
                        uv.setNextReviewAt(now.plusDays(1));
                    }
                } else {
                    uv.setWrongCount((uv.getWrongCount() != null ? uv.getWrongCount() : 0) + 1);
                    if (uv.getStatus() == null || uv.getStatus() == VocabularyStatus.NEW) {
                        uv.setStatus(VocabularyStatus.LEARNING);
                    }
                    uv.setNextReviewAt(now.plusHours(4));
                }
            } else {
                VocabularyStatus initialStatus = ("easy".equals(rating) || "MASTERED".equals(requestedStatus))
                        ? VocabularyStatus.MASTERED
                        : VocabularyStatus.LEARNING;
                uv = UserVocabulary.builder()
                        .user(user)
                        .normalizedWord(normalizedWord)
                        .displayWord(displayWord)
                        .status(initialStatus)
                        .reviewCount(1)
                        .correctCount(isPositive ? 1 : 0)
                        .wrongCount(isPositive ? 0 : 1)
                        .lastReviewedAt(now)
                        .nextReviewAt(isPositive ? now.plusDays(1) : now.plusHours(4))
                        .build();
            }
            userVocabularyRepository.save(uv);
        }
    }

    private Topic resolveTopic(String topicName) {
        if (topicName == null || topicName.isBlank()) {
            return null;
        }
        String trimmed = topicName.trim();
        Optional<Topic> exact = topicRepository.findByName(trimmed);
        if (exact.isPresent()) {
            return exact.get();
        }
        try {
            List<Topic> allTopics = topicRepository.findAll();
            if (allTopics != null && !allTopics.isEmpty()) {
                for (Topic t : allTopics) {
                    if (t != null && t.getName() != null && t.getName().trim().equalsIgnoreCase(trimmed)) {
                        return t;
                    }
                }
                String lower = trimmed.toLowerCase();
                for (Topic t : allTopics) {
                    if (t != null && t.getName() != null && t.getName().toLowerCase().contains(lower)) {
                        return t;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private VocabularySense resolveSenseForTopic(Vocabulary vocab, Topic topic, String topicName) {
        if (vocab == null || vocab.getSenses() == null || vocab.getSenses().isEmpty()) {
            return null;
        }
        if (topic != null && topic.getId() != null) {
            for (VocabularySense s : vocab.getSenses()) {
                if (s != null && s.getTopic() != null && topic.getId().equals(s.getTopic().getId())) {
                    return s;
                }
            }
            return vocab.getSenses().get(0);
        }
        if (topicName != null && !topicName.isBlank()) {
            String trimmed = topicName.trim();
            for (VocabularySense s : vocab.getSenses()) {
                if (s != null && s.getTopic() != null && s.getTopic().getName() != null
                        && s.getTopic().getName().trim().equalsIgnoreCase(trimmed)) {
                    return s;
                }
            }
        }
        return vocab.getSenses().get(0);
    }

    private String extractAudioUrl(Vocabulary vocab) {
        if (vocab.getPronunciations() == null || vocab.getPronunciations().isEmpty()) {
            return null;
        }
        return vocab.getPronunciations().get(0).getAudioUrl();
    }

    private PronunciationPair extractPronunciations(Vocabulary vocab) {
        String usAudio = null;
        String ukAudio = null;
        String usIpa = null;
        String ukIpa = null;

        if (vocab.getPronunciations() != null) {
            for (VocabularyPronunciation p : vocab.getPronunciations()) {
                if ("US".equalsIgnoreCase(p.getAccent())) {
                    usAudio = p.getAudioUrl();
                    usIpa = p.getIpa();
                } else if ("UK".equalsIgnoreCase(p.getAccent())) {
                    ukAudio = p.getAudioUrl();
                    ukIpa = p.getIpa();
                }
            }
            if (usAudio == null && !vocab.getPronunciations().isEmpty()) {
                usAudio = vocab.getPronunciations().get(0).getAudioUrl();
            }
            if (usIpa == null && !vocab.getPronunciations().isEmpty()) {
                usIpa = vocab.getPronunciations().get(0).getIpa();
            }
        }

        String generalAudio = usAudio != null ? usAudio : ukAudio;
        String generalIpa = usIpa != null ? usIpa : ukIpa;

        return new PronunciationPair(generalAudio, usAudio, ukAudio, generalIpa, usIpa, ukIpa);
    }

    private record PronunciationPair(
            String generalAudio,
            String usAudio,
            String ukAudio,
            String generalIpa,
            String usIpa,
            String ukIpa
    ) {}
}

package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.practice.FillBlankQuestionDto;
import com.parrotalk.backend.dto.practice.LearnWordDto;
import com.parrotalk.backend.dto.practice.MultipleChoiceQuestionDto;
import com.parrotalk.backend.dto.practice.TypeWordQuestionDto;
import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.TopicPracticeSentence;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.TopicPracticeSentenceRepository;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VocabularyPracticeSequentialCachingTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicPracticeSentenceRepository sentenceRepository;

    @Mock
    private TopicPracticeSentenceAiService aiService;

    @InjectMocks
    private VocabularyPracticeService practiceService;

    private Topic testTopic;
    private Vocabulary testVocab;
    private VocabularySense testSense;

    @BeforeEach
    void setUp() {
        testTopic = Topic.builder()
                .id(UUID.randomUUID())
                .name("Environment")
                .build();

        testSense = VocabularySense.builder()
                .id(UUID.randomUUID())
                .partOfSpeech("verb")
                .cefrLevel("C1")
                .definitionEn("to make something less severe or damaging")
                .definitionVn("làm giảm thiểu, giảm nhẹ")
                .exampleEn("Planting trees helps mitigate soil erosion.")
                .exampleVn("Trồng cây giúp giảm thiểu xói mòn đất.")
                .build();

        testVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("mitigate")
                .senses(new ArrayList<>(List.of(testSense)))
                .build();
    }

    @Test
    @DisplayName("AC1 AI Data Flow & Persistence: DB Miss triggers AI -> DB persist -> subsequent request DB Hit serves from cache")
    void testSequentialCachingFlow_DbMissThenDbHit() {
        // Arrange an in-memory repository store simulation
        Map<String, TopicPracticeSentence> dbStore = new HashMap<>();

        when(topicRepository.findByName("Environment")).thenReturn(Optional.of(testTopic));
        when(vocabularyRepository.findRandomVocabularies(eq("Environment"), isNull(), anyInt()))
                .thenReturn(List.of(testVocab));

        TopicPracticeSentence aiSentence = TopicPracticeSentence.builder()
                .id(UUID.randomUUID())
                .vocabulary(testVocab)
                .topic(testTopic)
                .targetWord("mitigate")
                .targetForm("mitigate")
                .sentenceWithBlank("Local authorities took action to ______ the disaster risk.")
                .completedSentence("Local authorities took action to mitigate the disaster risk.")
                .sentenceVn("Chính quyền địa phương đã hành động để giảm thiểu nguy cơ thảm họa.")
                .grammarContextTip("Follows infinitive marker 'to' -> base verb.")
                .explanation("'mitigate' means to reduce severity.")
                .build();

        when(aiService.generateSentence(eq(testVocab), eq(testTopic), eq(testSense)))
                .thenReturn(aiSentence);

        // Dynamic behavior for sentenceRepository simulating DB state
        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(eq(testTopic.getId()), eq(testVocab.getId())))
                .thenAnswer(inv -> Optional.ofNullable(dbStore.get(testTopic.getId() + "_" + testVocab.getId())));

        when(sentenceRepository.findFirstByTopicIdAndTargetWord(eq(testTopic.getId()), eq("mitigate")))
                .thenAnswer(inv -> Optional.ofNullable(dbStore.get(testTopic.getId() + "_mitigate")));

        when(sentenceRepository.findFirstByVocabularyId(eq(testVocab.getId())))
                .thenAnswer(inv -> Optional.ofNullable(dbStore.get("vocab_" + testVocab.getId())));

        when(sentenceRepository.findFirstByTargetWord(eq("mitigate")))
                .thenAnswer(inv -> Optional.ofNullable(dbStore.get("word_mitigate")));

        when(sentenceRepository.save(any(TopicPracticeSentence.class)))
                .thenAnswer(inv -> {
                    TopicPracticeSentence entity = inv.getArgument(0);
                    dbStore.put(testTopic.getId() + "_" + testVocab.getId(), entity);
                    dbStore.put(testTopic.getId() + "_mitigate", entity);
                    return entity;
                });

        // STEP 1: First invocation (DB MISS)
        List<FillBlankQuestionDto> firstResult = practiceService.getFillBlankQuestions("Environment", 5);

        assertNotNull(firstResult);
        assertEquals(1, firstResult.size());
        FillBlankQuestionDto q1 = firstResult.get(0);
        assertEquals("AI_GENERATED", q1.getSource());
        assertEquals("mitigate", q1.getWord());
        assertEquals("Local authorities took action to ______ the disaster risk.", q1.getSentenceWithBlank());

        // Verify AI service was called once, and sentence was saved to DB
        verify(aiService, times(1)).generateSentence(testVocab, testTopic, testSense);
        verify(sentenceRepository, times(1)).save(aiSentence);
        assertTrue(dbStore.containsKey(testTopic.getId() + "_" + testVocab.getId()));

        // STEP 2: Second invocation for the same topic and word (DB HIT)
        List<FillBlankQuestionDto> secondResult = practiceService.getFillBlankQuestions("Environment", 5);

        assertNotNull(secondResult);
        assertEquals(1, secondResult.size());
        FillBlankQuestionDto q2 = secondResult.get(0);
        assertEquals("DATABASE", q2.getSource(), "Subsequent request MUST hit database cache");
        assertEquals("mitigate", q2.getWord());
        assertEquals("Local authorities took action to ______ the disaster risk.", q2.getSentenceWithBlank());

        // Verify AI service was NOT invoked a second time
        verify(aiService, times(1)).generateSentence(any(), any(), any());
        verify(sentenceRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Resilience: DB Save failure does not crash practice generation or throw 500")
    void testGetFillBlankQuestions_DbSaveFails_GracefullyReturnsQuestion() {
        when(topicRepository.findByName("Environment")).thenReturn(Optional.of(testTopic));
        when(vocabularyRepository.findRandomVocabularies("Environment", null, 20))
                .thenReturn(List.of(testVocab));

        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(any(), any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTopicIdAndTargetWord(any(), any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByVocabularyId(any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTargetWord(any())).thenReturn(Optional.empty());

        TopicPracticeSentence aiSentence = TopicPracticeSentence.builder()
                .vocabulary(testVocab)
                .targetWord("mitigate")
                .targetForm("mitigate")
                .sentenceWithBlank("To ______ emissions, green energy is essential.")
                .completedSentence("To mitigate emissions, green energy is essential.")
                .build();

        when(aiService.generateSentence(any(), any(), any())).thenReturn(aiSentence);
        // Simulate DB write failure
        when(sentenceRepository.save(any())).thenThrow(new RuntimeException("DB Connection Reset / Disk Full"));

        List<FillBlankQuestionDto> result = practiceService.getFillBlankQuestions("Environment", 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AI_GENERATED", result.get(0).getSource());
        assertEquals("To ______ emissions, green energy is essential.", result.get(0).getSentenceWithBlank());
    }

    @Test
    @DisplayName("Edge Case: Non-existent topic returns empty list without error")
    void testGetFillBlankQuestions_NonExistentTopic_ReturnsEmptyList() {
        when(topicRepository.findByName("NonExistentTopicXYZ")).thenReturn(Optional.empty());
        when(vocabularyRepository.findRandomVocabularies("NonExistentTopicXYZ", null, 20))
                .thenReturn(List.of());

        List<FillBlankQuestionDto> result = practiceService.getFillBlankQuestions("NonExistentTopicXYZ", 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Edge Case: Limit boundary testing (negative, zero, overly large)")
    void testLimitBoundaries() {
        when(topicRepository.findByName("Environment")).thenReturn(Optional.of(testTopic));
        when(vocabularyRepository.findRandomVocabularies("Environment", null, 2))
                .thenReturn(List.of(testVocab));
        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(any(), any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTopicIdAndTargetWord(any(), any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByVocabularyId(any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTargetWord(any())).thenReturn(Optional.empty());

        TopicPracticeSentence sentence = TopicPracticeSentence.builder()
                .targetWord("mitigate")
                .targetForm("mitigate")
                .sentenceWithBlank("______")
                .completedSentence("mitigate")
                .build();
        when(aiService.generateSentence(any(), any(), any())).thenReturn(sentence);
        when(sentenceRepository.save(any())).thenReturn(sentence);

        // Negative limit -> bounded to 1 (boundedLimit = Math.max(1, Math.min(-5, 50)) -> 1, query limit * 2 = 2)
        List<FillBlankQuestionDto> negativeResult = practiceService.getFillBlankQuestions("Environment", -5);
        assertEquals(1, negativeResult.size());

        // Zero limit -> bounded to 1
        List<FillBlankQuestionDto> zeroResult = practiceService.getFillBlankQuestions("Environment", 0);
        assertEquals(1, zeroResult.size());

        // Overly large limit -> bounded to 50
        when(vocabularyRepository.findRandomVocabularies("Environment", null, 100))
                .thenReturn(List.of(testVocab));
        List<FillBlankQuestionDto> largeResult = practiceService.getFillBlankQuestions("Environment", 9999);
        assertEquals(1, largeResult.size());
    }

    @Test
    @DisplayName("Edge Case: Multiple Choice distractor padding when topic has only 1 word")
    void testMultipleChoice_SingleWordTopic_PadsOptionsWithoutCrash() {
        when(vocabularyRepository.findRandomVocabularies("Environment", null, 8))
                .thenReturn(List.of(testVocab));

        List<MultipleChoiceQuestionDto> result = practiceService.getMultipleChoiceQuestions("Environment", 4);

        assertNotNull(result);
        assertEquals(1, result.size());
        MultipleChoiceQuestionDto q = result.get(0);
        assertEquals("mitigate", q.getTargetWord());
        assertEquals(4, q.getOptions().size(), "Must pad options to exactly 4");
        assertTrue(q.getOptions().contains(q.getCorrectAnswer()));
        assertEquals(q.getCorrectAnswer(), q.getOptions().get(q.getCorrectIndex()));
    }

    @Test
    @DisplayName("Edge Case: Type the Word with 1-character target word")
    void testTypeWord_SingleCharacterWord() {
        Vocabulary oneLetterVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("a")
                .commonMeaningVn("một")
                .senses(List.of(VocabularySense.builder()
                        .definitionEn("used before a singular noun")
                        .exampleEn("It is a book.")
                        .build()))
                .build();

        when(vocabularyRepository.findRandomVocabularies("Grammar", null, 1))
                .thenReturn(List.of(oneLetterVocab));

        List<TypeWordQuestionDto> result = practiceService.getTypeWordQuestions("Grammar", 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        TypeWordQuestionDto q = result.get(0);
        assertEquals("a", q.getTargetWord());
        assertEquals("a", q.getFirstLetter());
        assertEquals(1, q.getCharacterCount());
        assertEquals("It is ________ book.", q.getExampleWithMask());
    }

    @Test
    @DisplayName("Edge Case: Vocabulary with null senses and null pronunciations")
    void testLearnWords_NullSensesAndPronunciations_NoNullPointerException() {
        Vocabulary bareVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("bare")
                .commonMeaningVn("trần trụi")
                .senses(new ArrayList<>())
                .pronunciations(null)
                .build();

        when(vocabularyRepository.findRandomVocabularies("General", null, 1))
                .thenReturn(List.of(bareVocab));

        List<LearnWordDto> cards = practiceService.getLearnWordCards("General", 1);

        assertNotNull(cards);
        assertEquals(1, cards.size());
        LearnWordDto card = cards.get(0);
        assertEquals("bare", card.getWord());
        assertEquals("bare", card.getDefinitionEn());
        assertEquals("trần trụi", card.getDefinitionVn());
        assertNull(card.getPhonetic());
        assertNull(card.getAudioUrl());
        assertTrue(card.getSynonyms().isEmpty());
    }
}

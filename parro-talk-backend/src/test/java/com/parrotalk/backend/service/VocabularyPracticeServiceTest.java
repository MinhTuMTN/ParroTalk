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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VocabularyPracticeServiceTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicPracticeSentenceRepository sentenceRepository;

    @Mock
    private TopicPracticeSentenceAiService aiService;

    @Mock
    private AiCoreClient aiCoreClient;

    @Mock
    private UserVocabularyRepository userVocabularyRepository;

    @InjectMocks
    private VocabularyPracticeService practiceService;

    private Topic businessTopic;
    private Vocabulary negotiateVocab;
    private VocabularySense negotiateSense;

    @BeforeEach
    void setUp() {
        businessTopic = Topic.builder()
                .id(UUID.randomUUID())
                .name("Business")
                .build();

        negotiateSense = VocabularySense.builder()
                .id(UUID.randomUUID())
                .partOfSpeech("verb")
                .cefrLevel("B2")
                .definitionEn("to discuss something in order to reach an agreement")
                .definitionVn("đàm phán, thương lượng")
                .exampleEn("The company agreed to negotiate the contract.")
                .exampleVn("Công ty đã đồng ý đàm phán hợp đồng.")
                .topic(businessTopic)
                .synonyms(new ArrayList<>(List.of(
                        SenseSynonym.builder().synonymWord("bargain").build(),
                        SenseSynonym.builder().synonymWord("confer").build()
                )))
                .antonyms(new ArrayList<>(List.of(
                        SenseAntonym.builder().antonymWord("refuse").build()
                )))
                .collocations(new ArrayList<>(List.of(
                        SenseCollocation.builder().collocationWord("negotiate a deal").build()
                )))
                .build();

        VocabularyPronunciation pronUs = VocabularyPronunciation.builder()
                .id(UUID.randomUUID())
                .accent("US")
                .ipa("/nəˈɡoʊ.ʃi.eɪt/")
                .audioUrl("https://audio.parrotalk.fun/us/negotiate.mp3")
                .build();

        VocabularyPronunciation pronUk = VocabularyPronunciation.builder()
                .id(UUID.randomUUID())
                .accent("UK")
                .ipa("/nəˈɡəʊ.ʃi.eɪt/")
                .audioUrl("https://audio.parrotalk.fun/uk/negotiate.mp3")
                .build();

        negotiateVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("negotiate")
                .normalizedWord("negotiate")
                .commonMeaningVn("đàm phán")
                .senses(new ArrayList<>(List.of(negotiateSense)))
                .pronunciations(new ArrayList<>(List.of(pronUs, pronUk)))
                .build();
    }

    @Test
    @DisplayName("R1 Hybrid Flow - DB Hit: Returns pre-existing sentence from repository without invoking AI")
    void testGetFillBlankQuestions_DbHit_ReturnsCachedWithoutAi() {
        // Arrange
        when(topicRepository.findByName("Business")).thenReturn(Optional.of(businessTopic));
        when(vocabularyRepository.findRandomVocabularies("Business", null, 20))
                .thenReturn(List.of(negotiateVocab));

        TopicPracticeSentence cached = TopicPracticeSentence.builder()
                .id(UUID.randomUUID())
                .vocabulary(negotiateVocab)
                .topic(businessTopic)
                .targetWord("negotiate")
                .targetForm("negotiate")
                .sentenceWithBlank("The company agreed to ______ the contract.")
                .completedSentence("The company agreed to negotiate the contract.")
                .sentenceVn("Công ty đã đồng ý đàm phán hợp đồng.")
                .grammarContextTip("Follows infinitive marker 'to' -> requires base form verb.")
                .explanation("'negotiate' is a transitive verb meaning to discuss terms.")
                .build();

        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(businessTopic.getId(), negotiateVocab.getId()))
                .thenReturn(Optional.of(cached));

        // Act
        List<FillBlankQuestionDto> result = practiceService.getFillBlankQuestions("Business", 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        FillBlankQuestionDto q = result.get(0);
        assertEquals("DATABASE", q.getSource());
        assertEquals("negotiate", q.getWord());
        assertEquals("negotiate", q.getTargetForm());
        assertEquals("The company agreed to ______ the contract.", q.getSentenceWithBlank());
        assertEquals("Follows infinitive marker 'to' -> requires base form verb.", q.getGrammarContextTip());
        assertEquals("to discuss something in order to reach an agreement", q.getDefinitionEn());
        assertEquals("đàm phán, thương lượng", q.getDefinitionVn());
        assertEquals("B2", q.getCefrLevel());

        // Verify: AI was NEVER invoked, and no redundant DB save occurred
        verifyNoInteractions(aiService);
        verify(sentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("R1 Hybrid Flow - DB Miss: Calls AI service, persists newly generated sentence, and returns it")
    void testGetFillBlankQuestions_DbMiss_CallsAiAndPersists() {
        // Arrange
        when(topicRepository.findByName("Business")).thenReturn(Optional.of(businessTopic));
        when(vocabularyRepository.findRandomVocabularies("Business", null, 20))
                .thenReturn(List.of(negotiateVocab));

        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(businessTopic.getId(), negotiateVocab.getId()))
                .thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTopicIdAndTargetWord(businessTopic.getId(), "negotiate"))
                .thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByVocabularyId(negotiateVocab.getId()))
                .thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTargetWord("negotiate"))
                .thenReturn(Optional.empty());

        TopicPracticeSentence aiGenerated = TopicPracticeSentence.builder()
                .vocabulary(negotiateVocab)
                .topic(businessTopic)
                .targetWord("negotiate")
                .targetForm("negotiating")
                .sentenceWithBlank("Both sides are currently ______ terms for the partnership.")
                .completedSentence("Both sides are currently negotiating terms for the partnership.")
                .sentenceVn("Cả hai bên hiện đang đàm phán các điều khoản cho mối quan hệ đối tác.")
                .grammarContextTip("Present continuous auxiliary 'are' -> requires present participle '-ing'.")
                .explanation("'negotiating' describes an ongoing discussion in business.")
                .build();

        when(aiService.generateSentence(eq(negotiateVocab), eq(businessTopic), eq(negotiateSense)))
                .thenReturn(aiGenerated);

        UUID savedId = UUID.randomUUID();
        when(sentenceRepository.save(any(TopicPracticeSentence.class)))
                .thenAnswer(invocation -> {
                    TopicPracticeSentence entity = invocation.getArgument(0);
                    entity.setId(savedId);
                    return entity;
                });

        // Act
        List<FillBlankQuestionDto> result = practiceService.getFillBlankQuestions("Business", 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        FillBlankQuestionDto q = result.get(0);
        assertEquals("AI_GENERATED", q.getSource());
        assertEquals("negotiate", q.getWord());
        assertEquals("negotiating", q.getTargetForm());
        assertEquals("Both sides are currently ______ terms for the partnership.", q.getSentenceWithBlank());
        assertEquals(savedId, q.getId());

        // Verify: AI was invoked and sentence was persisted to DB
        verify(aiService, times(1)).generateSentence(negotiateVocab, businessTopic, negotiateSense);
        verify(sentenceRepository, times(1)).save(aiGenerated);
    }

    @Test
    @DisplayName("R1 Hybrid Flow - Fallback: When AI fails, returns parsed sense example with grammar context")
    void testGetFillBlankQuestions_AiFails_FallsBackToSenseExample() {
        // Arrange
        when(topicRepository.findByName("Business")).thenReturn(Optional.of(businessTopic));
        when(vocabularyRepository.findRandomVocabularies("Business", null, 20))
                .thenReturn(List.of(negotiateVocab));

        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(businessTopic.getId(), negotiateVocab.getId()))
                .thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTopicIdAndTargetWord(businessTopic.getId(), "negotiate"))
                .thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByVocabularyId(negotiateVocab.getId()))
                .thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTargetWord("negotiate"))
                .thenReturn(Optional.empty());

        when(aiService.generateSentence(eq(negotiateVocab), eq(businessTopic), eq(negotiateSense)))
                .thenThrow(new RuntimeException("Groq API rate limit or network unreachable"));

        TopicPracticeSentence fallbackSentence = TopicPracticeSentence.builder()
                .id(UUID.randomUUID())
                .vocabulary(negotiateVocab)
                .topic(businessTopic)
                .targetWord("negotiate")
                .targetForm("negotiate")
                .sentenceWithBlank("The company agreed to ______ the contract.")
                .completedSentence("The company agreed to negotiate the contract.")
                .sentenceVn("Công ty đã đồng ý đàm phán hợp đồng.")
                .grammarContextTip("Identify the correct form and part of speech (verb) that fits this sentence context.")
                .explanation("'negotiate' (verb): to discuss something. Fits contextually within the Business theme.")
                .build();

        when(aiService.buildFallbackSentence(eq(negotiateVocab), eq(businessTopic), eq(negotiateSense)))
                .thenReturn(fallbackSentence);

        // Act
        List<FillBlankQuestionDto> result = practiceService.getFillBlankQuestions("Business", 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        FillBlankQuestionDto q = result.get(0);
        assertEquals("The company agreed to ______ the contract.", q.getSentenceWithBlank());
        assertTrue(q.getGrammarContextTip().contains("verb"));
        assertTrue(q.getExplanation().contains("Business"));

        verify(aiService, times(1)).buildFallbackSentence(negotiateVocab, businessTopic, negotiateSense);
        verify(sentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("R1 Hybrid Flow - Selects topic-matching sense and skips redundant AI calls when AI is offline")
    void testGetFillBlankQuestions_TopicSenseSelectionAndCircuitBreaker() {
        Topic otherTopic = Topic.builder().id(UUID.randomUUID()).name("General").build();
        VocabularySense wrongTopicSense = VocabularySense.builder()
                .id(UUID.randomUUID())
                .partOfSpeech("noun")
                .cefrLevel("A1")
                .definitionEn("wrong topic sense definition")
                .definitionVn("nghĩa sai chủ đề")
                .topic(otherTopic)
                .build();

        Vocabulary multiSenseVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("negotiate")
                .commonMeaningVn("đàm phán")
                .senses(new ArrayList<>(List.of(wrongTopicSense, negotiateSense)))
                .build();

        Vocabulary secondVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("deadline")
                .commonMeaningVn("hạn chót")
                .senses(new ArrayList<>(List.of(VocabularySense.builder()
                        .id(UUID.randomUUID())
                        .partOfSpeech("noun")
                        .cefrLevel("B1")
                        .definitionEn("a time or day by which something must be done")
                        .definitionVn("hạn chót hoàn thành")
                        .topic(businessTopic)
                        .build())))
                .build();

        when(topicRepository.findByName("Business")).thenReturn(Optional.of(businessTopic));
        when(vocabularyRepository.findRandomVocabularies("Business", null, 20))
                .thenReturn(List.of(multiSenseVocab, secondVocab));

        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(any(), any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTopicIdAndTargetWord(any(), any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByVocabularyId(any())).thenReturn(Optional.empty());
        when(sentenceRepository.findFirstByTargetWord(any())).thenReturn(Optional.empty());

        TopicPracticeSentence fallback1 = TopicPracticeSentence.builder()
                .vocabulary(multiSenseVocab)
                .topic(businessTopic)
                .targetWord("negotiate")
                .targetForm("negotiate")
                .sentenceWithBlank("The company agreed to ______ the contract.")
                .completedSentence("The company agreed to negotiate the contract.")
                .fallback(true)
                .build();

        TopicPracticeSentence fallback2 = TopicPracticeSentence.builder()
                .vocabulary(secondVocab)
                .topic(businessTopic)
                .targetWord("deadline")
                .targetForm("deadline")
                .sentenceWithBlank("We must meet the project ______.")
                .completedSentence("We must meet the project deadline.")
                .fallback(true)
                .build();

        // First AI call returns a fallback (indicating AI failed internally)
        when(aiService.generateSentence(eq(multiSenseVocab), eq(businessTopic), eq(negotiateSense)))
                .thenReturn(fallback1);
        when(aiService.buildFallbackSentence(eq(secondVocab), eq(businessTopic), any()))
                .thenReturn(fallback2);

        List<FillBlankQuestionDto> result = practiceService.getFillBlankQuestions("Business", 10);

        assertEquals(2, result.size());
        // Must have selected negotiateSense (Business, B2) rather than wrongTopicSense (General, A1)
        assertEquals("đàm phán, thương lượng", result.get(0).getDefinitionVn());
        assertEquals("B2", result.get(0).getCefrLevel());
        assertEquals("verb", result.get(0).getPartOfSpeech());

        // Because the first AI call fell back, the second word must go straight to buildFallbackSentence without calling generateSentence
        verify(aiService, times(1)).generateSentence(any(), any(), any());
        verify(aiService, times(1)).buildFallbackSentence(eq(secondVocab), eq(businessTopic), any());
        verify(sentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mode 1: Learn Words generates rich reversible flashcards with phonetic, audio, and collocations")
    void testGetLearnWordCards_Success() {
        // Arrange
        when(vocabularyRepository.findRandomVocabularies("Business", null, 10))
                .thenReturn(List.of(negotiateVocab));

        // Act
        List<LearnWordDto> cards = practiceService.getLearnWordCards("Business", 10);

        // Assert
        assertNotNull(cards);
        assertEquals(1, cards.size());
        LearnWordDto card = cards.get(0);
        assertEquals("negotiate", card.getWord());
        assertEquals("/nəˈɡoʊ.ʃi.eɪt/", card.getPhonetic());
        assertEquals("/nəˈɡoʊ.ʃi.eɪt/", card.getPhoneticUs());
        assertEquals("/nəˈɡəʊ.ʃi.eɪt/", card.getPhoneticUk());
        assertEquals("https://audio.parrotalk.fun/us/negotiate.mp3", card.getAudioUrlUs());
        assertEquals("https://audio.parrotalk.fun/uk/negotiate.mp3", card.getAudioUrlUk());
        assertEquals("verb", card.getPartOfSpeech());
        assertEquals("B2", card.getCefrLevel());
        assertEquals("Business", card.getTopic());
        assertEquals("to discuss something in order to reach an agreement", card.getDefinitionEn());
        assertEquals("đàm phán, thương lượng", card.getDefinitionVn());
        assertThat(card.getSynonyms()).contains("bargain", "confer");
        assertThat(card.getAntonyms()).contains("refuse");
        assertThat(card.getCollocations()).contains("negotiate a deal");
    }

    @Test
    @DisplayName("Mode 2: Multiple Choice generates randomized options with topic-relevant distractors")
    void testGetMultipleChoiceQuestions_Success() {
        // Arrange
        Vocabulary secondVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("collaborate")
                .commonMeaningVn("hợp tác")
                .senses(List.of(VocabularySense.builder()
                        .definitionEn("to work together with someone else")
                        .partOfSpeech("verb")
                        .build()))
                .build();

        Vocabulary thirdVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("delegate")
                .commonMeaningVn("ủy quyền")
                .senses(List.of(VocabularySense.builder()
                        .definitionEn("to give a particular job or duty to someone else")
                        .partOfSpeech("verb")
                        .build()))
                .build();

        Vocabulary fourthVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("terminate")
                .commonMeaningVn("chấm dứt")
                .senses(List.of(VocabularySense.builder()
                        .definitionEn("to end or make something end")
                        .partOfSpeech("verb")
                        .build()))
                .build();

        when(vocabularyRepository.findRandomVocabularies("Business", null, 8))
                .thenReturn(List.of(negotiateVocab, secondVocab, thirdVocab, fourthVocab));

        // Act
        List<MultipleChoiceQuestionDto> questions = practiceService.getMultipleChoiceQuestions("Business", 4);

        // Assert
        assertNotNull(questions);
        assertEquals(4, questions.size());

        MultipleChoiceQuestionDto q = questions.get(0);
        assertEquals("What is the meaning of 'negotiate'?", q.getQuestion());
        assertEquals("negotiate", q.getTargetWord());
        assertEquals(4, q.getOptions().size());
        assertEquals("to discuss something in order to reach an agreement", q.getCorrectAnswer());
        assertTrue(q.getOptions().contains(q.getCorrectAnswer()));
        assertEquals(q.getCorrectAnswer(), q.getOptions().get(q.getCorrectIndex()));
    }

    @Test
    @DisplayName("Mode 3: Type the Word generates active recall with masked example and first-letter clue")
    void testGetTypeWordQuestions_Success() {
        // Arrange
        when(vocabularyRepository.findRandomVocabularies("Business", null, 5))
                .thenReturn(List.of(negotiateVocab));

        // Act
        List<TypeWordQuestionDto> questions = practiceService.getTypeWordQuestions("Business", 5);

        // Assert
        assertNotNull(questions);
        assertEquals(1, questions.size());
        TypeWordQuestionDto q = questions.get(0);
        assertEquals("negotiate", q.getTargetWord());
        assertEquals("n", q.getFirstLetter());
        assertEquals(9, q.getCharacterCount());
        assertTrue(q.getPromptDefinition().contains("to discuss something"));
        assertTrue(q.getExampleWithMask().contains("________"));
        assertFalse(q.getExampleWithMask().toLowerCase().contains("negotiate"));
    }

    @Test
    @DisplayName("Backward Compatibility: generateQuestions routes by mode parameter and preserves legacy output")
    void testBackwardCompatibility_ModeRouting() {
        // Arrange
        when(topicRepository.findByName("Business")).thenReturn(Optional.of(businessTopic));
        when(vocabularyRepository.findRandomVocabularies("Business", null, 20))
                .thenReturn(List.of(negotiateVocab));
        TopicPracticeSentence cached = TopicPracticeSentence.builder()
                .sentenceWithBlank("The company agreed to ______ the contract.")
                .completedSentence("The company agreed to negotiate the contract.")
                .targetForm("negotiate")
                .explanation("Grammar note")
                .build();
        when(sentenceRepository.findFirstByTopicIdAndVocabularyId(businessTopic.getId(), negotiateVocab.getId()))
                .thenReturn(Optional.of(cached));

        // Act: fill-blank mode
        List<VocabularyPracticeQuestionDto> fbResult = practiceService.generateQuestions("Business", null, 10, "fill-blank");
        assertNotNull(fbResult);
        assertEquals(1, fbResult.size());
        assertEquals("FILL_IN_BLANKS", fbResult.get(0).getQuestionType());
        assertEquals("The company agreed to ______ the contract.", fbResult.get(0).getSentenceWithBlank());

        // Act: getPracticeQuestions convenience
        when(vocabularyRepository.findRandomVocabularies("Business", null, 20))
                .thenReturn(List.of(negotiateVocab));
        List<VocabularyPracticeQuestionDto> legacyResult = practiceService.getPracticeQuestions("Business", 10);
        assertNotNull(legacyResult);
        assertFalse(legacyResult.isEmpty());
    }

    @Test
    @DisplayName("Word Progression: Excludes practiced and learned words and persists UserVocabulary on submitPractice")
    void testWordProgression_ExcludesLearnedAndPersistsSubmission() {
        User user = User.builder().id(UUID.randomUUID()).build();
        when(userVocabularyRepository.findLearnedNormalizedWordsByUserIdAndTopicName(user.getId(), "Business"))
                .thenReturn(List.of("compromise"));
        when(vocabularyRepository.findRandomVocabularies(
                eq("Business"),
                isNull(),
                eq(10),
                eq(List.of("deadline", "compromise"))))
                .thenReturn(List.of(negotiateVocab));

        List<LearnWordDto> cards = practiceService.getLearnWordCards("Business", 10, List.of("Deadline"), user);
        assertEquals(1, cards.size());
        assertEquals("negotiate", cards.get(0).getWord());

        when(userVocabularyRepository.findByUserIdAndNormalizedWord(user.getId(), "negotiate"))
                .thenReturn(Optional.empty());

        practiceService.submitPractice(
                List.of(Map.of("word", "negotiate", "isCorrect", true, "rating", "easy")),
                user
        );

        verify(userVocabularyRepository, times(1)).save(argThat((UserVocabulary uv) ->
                "negotiate".equals(uv.getNormalizedWord()) && uv.getStatus() == VocabularyStatus.MASTERED
        ));
    }
}

package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.TopicPracticeSentence;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.TopicPracticeSentenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopicPracticeSentenceAiServiceTest {

    @Mock
    private AiCoreClient aiCoreClient;

    @Mock
    private TopicPracticeSentenceRepository sentenceRepository;

    @InjectMocks
    private TopicPracticeSentenceAiService aiService;

    private Vocabulary vocab;
    private Topic topic;
    private VocabularySense sense;

    @BeforeEach
    void setUp() {
        topic = Topic.builder()
                .id(UUID.randomUUID())
                .name("Technology")
                .build();

        sense = VocabularySense.builder()
                .id(UUID.randomUUID())
                .partOfSpeech("verb")
                .definitionEn("to invent or create something new")
                .definitionVn("phát minh, sáng tạo")
                .exampleEn("Engineers innovate constantly to solve complex problems.")
                .exampleVn("Các kỹ sư không ngừng đổi mới để giải quyết các vấn đề phức tạp.")
                .build();

        vocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("innovate")
                .build();
    }

    @Test
    @DisplayName("AI Success: Generates sentence with complete fields and blank marker")
    @SuppressWarnings("unchecked")
    void testGenerateSentence_Success() {
        TopicPracticeSentenceAiService.AiSentenceResponse mockResponse = TopicPracticeSentenceAiService.AiSentenceResponse.builder()
                .targetWord("innovate")
                .targetForm("innovates")
                .sentenceWithBlank("The company ______ rapidly in the field of AI.")
                .completedSentence("The company innovates rapidly in the field of AI.")
                .sentenceVn("Công ty đổi mới nhanh chóng trong lĩnh vực AI.")
                .grammarContextTip("Third-person singular present -> requires -s suffix.")
                .explanation("'innovates' fits the third person subject.")
                .build();

        when(aiCoreClient.callAndValidate(anyString(), anyString(), eq(TopicPracticeSentenceAiService.AiSentenceResponse.class), any()))
                .thenAnswer(inv -> {
                    Function<TopicPracticeSentenceAiService.AiSentenceResponse, TopicPracticeSentenceAiService.AiSentenceResponse> validator = inv.getArgument(3);
                    return validator.apply(mockResponse);
                });

        TopicPracticeSentence result = aiService.generateSentence(vocab, topic, sense);

        assertNotNull(result);
        assertEquals("innovate", result.getTargetWord());
        assertEquals("innovates", result.getTargetForm());
        assertEquals("The company ______ rapidly in the field of AI.", result.getSentenceWithBlank());
        assertEquals("The company innovates rapidly in the field of AI.", result.getCompletedSentence());
        assertEquals("Công ty đổi mới nhanh chóng trong lĩnh vực AI.", result.getSentenceVn());
        assertEquals("Third-person singular present -> requires -s suffix.", result.getGrammarContextTip());
        assertEquals("'innovates' fits the third person subject.", result.getExplanation());
        assertEquals(vocab, result.getVocabulary());
        assertEquals(topic, result.getTopic());
    }

    @Test
    @DisplayName("AI Response Validator: TargetForm null is auto-populated from targetWord")
    @SuppressWarnings("unchecked")
    void testGenerateSentence_ValidatorAutofillsTargetForm() {
        TopicPracticeSentenceAiService.AiSentenceResponse mockResponse = TopicPracticeSentenceAiService.AiSentenceResponse.builder()
                .targetWord("innovate")
                .targetForm(null) // missing targetForm
                .sentenceWithBlank("Engineers must ______ daily.")
                .completedSentence(null) // missing completedSentence
                .build();

        when(aiCoreClient.callAndValidate(anyString(), anyString(), eq(TopicPracticeSentenceAiService.AiSentenceResponse.class), any()))
                .thenAnswer(inv -> {
                    Function<TopicPracticeSentenceAiService.AiSentenceResponse, TopicPracticeSentenceAiService.AiSentenceResponse> validator = inv.getArgument(3);
                    return validator.apply(mockResponse);
                });

        TopicPracticeSentence result = aiService.generateSentence(vocab, topic, sense);

        assertNotNull(result);
        assertEquals("innovate", result.getTargetForm());
        assertEquals("Engineers must innovate daily.", result.getCompletedSentence());
    }

    @Test
    @DisplayName("AI Response Validator: Missing blank token ______ triggers fallback")
    @SuppressWarnings("unchecked")
    void testGenerateSentence_ValidatorRejectsInvalidBlank_FallsBack() {
        TopicPracticeSentenceAiService.AiSentenceResponse invalidResponse = TopicPracticeSentenceAiService.AiSentenceResponse.builder()
                .targetWord("innovate")
                .targetForm("innovate")
                .sentenceWithBlank("This sentence forgot to include any blank at all.")
                .completedSentence("This sentence forgot to include any blank at all.")
                .build();

        when(aiCoreClient.callAndValidate(anyString(), anyString(), eq(TopicPracticeSentenceAiService.AiSentenceResponse.class), any()))
                .thenAnswer(inv -> {
                    Function<TopicPracticeSentenceAiService.AiSentenceResponse, TopicPracticeSentenceAiService.AiSentenceResponse> validator = inv.getArgument(3);
                    return validator.apply(invalidResponse); // validator throws IllegalArgumentException
                });

        TopicPracticeSentence result = aiService.generateSentence(vocab, topic, sense);

        // Should gracefully fall back to sense example
        assertNotNull(result);
        assertEquals("Engineers ______ constantly to solve complex problems.", result.getSentenceWithBlank());
        assertEquals("Engineers innovate constantly to solve complex problems.", result.getCompletedSentence());
    }

    @Test
    @DisplayName("AI Offline / Exception: Gracefully falls back to sense example without throwing 500")
    void testGenerateSentence_AiException_FallsBackGracefully() {
        when(aiCoreClient.callAndValidate(anyString(), anyString(), eq(TopicPracticeSentenceAiService.AiSentenceResponse.class), any()))
                .thenThrow(new RuntimeException("503 Service Unavailable: Groq downstream gateway timeout"));

        TopicPracticeSentence result = aiService.generateSentence(vocab, topic, sense);

        assertNotNull(result);
        assertEquals("innovate", result.getTargetWord());
        assertEquals("Engineers ______ constantly to solve complex problems.", result.getSentenceWithBlank());
        assertEquals("Engineers innovate constantly to solve complex problems.", result.getCompletedSentence());
        assertTrue(result.getGrammarContextTip().contains("verb"));
    }

    @Test
    @DisplayName("Fallback: Sense example contains inflected form of word -> masks inflected form and sets targetForm")
    void testBuildFallbackSentence_ExampleContainsInflectedForm() {
        Vocabulary issueVocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("issue")
                .build();
        VocabularySense issueSense = VocabularySense.builder()
                .id(UUID.randomUUID())
                .partOfSpeech("noun")
                .definitionEn("an important topic or problem for debate or discussion")
                .definitionVn("vấn đề, khía cạnh cần thảo luận")
                .exampleEn("The executive board addressed several financial issues this quarter.")
                .exampleVn("Hội đồng quản trị đã giải quyết một số vấn đề tài chính trong quý này.")
                .build();

        TopicPracticeSentence result = aiService.buildFallbackSentence(issueVocab, topic, issueSense);

        assertNotNull(result);
        assertEquals("issue", result.getTargetWord());
        assertEquals("issues", result.getTargetForm());
        assertEquals("The executive board addressed several financial ______ this quarter.", result.getSentenceWithBlank());
        assertEquals("The executive board addressed several financial issues this quarter.", result.getCompletedSentence());
        assertFalse(result.getSentenceWithBlank().toLowerCase().contains("issue"),
                "Sentence with blank must not leak the target word");
    }

    @Test
    @DisplayName("Fallback: Sense example does not contain word -> generates clean blank without leaking target word")
    void testBuildFallbackSentence_ExampleDoesNotContainWord() {
        sense.setExampleEn("New digital tools facilitate modern scientific research.");

        TopicPracticeSentence result = aiService.buildFallbackSentence(vocab, topic, sense);

        assertNotNull(result);
        assertTrue(result.getSentenceWithBlank().contains("______"),
                "Fallback sentence must contain the blank token ______");
        assertFalse(result.getSentenceWithBlank().toLowerCase().contains("innovate"),
                "Fallback sentence with blank must NEVER leak the target word 'innovate'");
    }

    @Test
    @DisplayName("AI Response Validator: Sanitizes grammarContextTip if it leaks targetForm or targetWord")
    @SuppressWarnings("unchecked")
    void testGenerateSentence_ValidatorSanitizesLeakedWordInGrammarTip() {
        TopicPracticeSentenceAiService.AiSentenceResponse mockResponse = TopicPracticeSentenceAiService.AiSentenceResponse.builder()
                .targetWord("innovate")
                .targetForm("innovates")
                .sentenceWithBlank("The company ______ rapidly in the field of AI.")
                .completedSentence("The company innovates rapidly in the field of AI.")
                .sentenceVn("Công ty đổi mới nhanh chóng trong lĩnh vực AI.")
                .grammarContextTip("Requires third-person singular verb 'innovates' after subject 'The company'.")
                .explanation("'innovates' fits the third person subject.")
                .build();

        when(aiCoreClient.callAndValidate(anyString(), anyString(), eq(TopicPracticeSentenceAiService.AiSentenceResponse.class), any()))
                .thenAnswer(inv -> {
                    Function<TopicPracticeSentenceAiService.AiSentenceResponse, TopicPracticeSentenceAiService.AiSentenceResponse> validator = inv.getArgument(3);
                    return validator.apply(mockResponse);
                });

        TopicPracticeSentence result = aiService.generateSentence(vocab, topic, sense);

        assertNotNull(result);
        assertFalse(result.getGrammarContextTip().toLowerCase().contains("innovate"),
                "Grammar hint must not reveal the target word or target form");
    }

    @Test
    @DisplayName("Fallback: Sense example is null/blank -> generates topic template fallback")
    void testBuildFallbackSentence_ExampleNull_GeneratesTopicTemplate() {
        sense.setExampleEn(null);
        sense.setExampleVn(null);

        TopicPracticeSentence result = aiService.buildFallbackSentence(vocab, topic, sense);

        assertNotNull(result);
        assertEquals("In the topic of Technology, the term ______ plays an important role.", result.getSentenceWithBlank());
        assertEquals("In the topic of Technology, the term innovate plays an important role.", result.getCompletedSentence());
    }

    @Test
    @DisplayName("Fallback: Sense is completely null -> does not throw NullPointerException")
    void testBuildFallbackSentence_SenseNull_NoNullPointerException() {
        TopicPracticeSentence result = aiService.buildFallbackSentence(vocab, topic, null);

        assertNotNull(result);
        assertEquals("innovate", result.getTargetWord());
        assertEquals("In the topic of Technology, the term ______ plays an important role.", result.getSentenceWithBlank());
        assertEquals("In the topic of Technology, the term innovate plays an important role.", result.getCompletedSentence());
    }

    @Test
    @DisplayName("Fallback: Topic is null -> defaults to 'General' topic without NPE")
    void testBuildFallbackSentence_TopicNull_DefaultsToGeneral() {
        TopicPracticeSentence result = aiService.buildFallbackSentence(vocab, null, null);

        assertNotNull(result);
        assertEquals("In the topic of General, the term ______ plays an important role.", result.getSentenceWithBlank());
        assertEquals("In the topic of General, the term innovate plays an important role.", result.getCompletedSentence());
    }

    @Test
    @DisplayName("Persist: generateAndPersistSentence saves generated entity to repository")
    void testGenerateAndPersistSentence_SavesToRepo() {
        TopicPracticeSentence fallback = aiService.buildFallbackSentence(vocab, topic, sense);
        when(aiCoreClient.callAndValidate(anyString(), anyString(), any(Class.class), any()))
                .thenThrow(new RuntimeException("Offline"));

        when(sentenceRepository.save(any(TopicPracticeSentence.class))).thenReturn(fallback);

        TopicPracticeSentence saved = aiService.generateAndPersistSentence(vocab, topic, sense);

        assertNotNull(saved);
        verify(sentenceRepository, times(1)).save(any(TopicPracticeSentence.class));
    }
}

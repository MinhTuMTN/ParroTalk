package com.parrotalk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.McqQuestionCategory;
import com.parrotalk.backend.constant.QuestionGenerationSource;
import com.parrotalk.backend.constant.UserNotificationType;
import com.parrotalk.backend.constant.VocabularyListStatus;
import com.parrotalk.backend.entity.GrammarMultipleChoiceQuestion;
import com.parrotalk.backend.entity.Idiom;
import com.parrotalk.backend.entity.SenseAntonym;
import com.parrotalk.backend.entity.SenseCollocation;
import com.parrotalk.backend.entity.SenseSynonym;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyList;
import com.parrotalk.backend.entity.VocabularyListItem;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.GrammarMultipleChoiceQuestionRepository;
import com.parrotalk.backend.repository.IdiomRepository;
import com.parrotalk.backend.repository.PhrasalVerbRepository;
import com.parrotalk.backend.repository.VocabularyListItemRepository;
import com.parrotalk.backend.repository.VocabularyListRepository;
import com.parrotalk.backend.repository.VocabularySenseRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomListQuestionBatchServiceTest {

    @Mock
    private AiCoreClient aiCoreClient;

    @Mock
    private GrammarMultipleChoiceQuestionRepository mcqRepository;

    @Mock
    private VocabularyListRepository listRepository;

    @Mock
    private VocabularyListItemRepository listItemRepository;

    @Mock
    private VocabularySenseRepository senseRepository;

    @Mock
    private IdiomRepository idiomRepository;

    @Mock
    private PhrasalVerbRepository phrasalVerbRepository;

    @Mock
    private UserNotificationService notificationService;

    private CustomListQuestionBatchService batchService;

    @BeforeEach
    void setUp() {
        batchService = new CustomListQuestionBatchService(
                aiCoreClient,
                mcqRepository,
                listRepository,
                listItemRepository,
                senseRepository,
                idiomRepository,
                phrasalVerbRepository,
                notificationService,
                new ObjectMapper()
        );
    }

    @Test
    void determineRequiredCategoriesShouldAdaptToSenseData() {
        Vocabulary vocab = Vocabulary.builder().id(UUID.randomUUID()).word("resilient").build();
        VocabularySense richSense = VocabularySense.builder()
                .id(UUID.randomUUID())
                .vocabulary(vocab)
                .partOfSpeech("adjective")
                .cefrLevel("B2")
                .definitionEn("able to recover quickly")
                .build();
        richSense.getSynonyms().add(SenseSynonym.builder().sense(richSense).synonymWord("tough").build());
        richSense.getAntonyms().add(SenseAntonym.builder().sense(richSense).antonymWord("fragile").build());
        richSense.getCollocations().add(SenseCollocation.builder().sense(richSense).collocationWord("resilient economy").build());

        Set<McqQuestionCategory> categories = batchService.determineRequiredCategories(richSense);
        assertThat(categories).containsExactlyInAnyOrder(
                McqQuestionCategory.CONTEXTUAL_CLOZE,
                McqQuestionCategory.CONFUSING_NUANCE,
                McqQuestionCategory.WORD_FORM,
                McqQuestionCategory.GRAMMAR_PATTERN,
                McqQuestionCategory.COLLOCATION_PREPOSITION,
                McqQuestionCategory.SYNONYM_IN_CONTEXT,
                McqQuestionCategory.ANTONYM_IN_CONTEXT
        );
    }

    @Test
    void finalizeListShouldSkipSenseWithEnoughQuestionsAndGenerateMissingWithFinalizeSourceFlag() {
        UUID listId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("owner@test.com").fullName("Owner").build();

        Vocabulary vocab = Vocabulary.builder().id(UUID.randomUUID()).word("resilient").normalizedWord("resilient").build();
        UUID cachedSenseId = UUID.randomUUID();
        UUID newSenseId = UUID.randomUUID();
        UUID idiomId = UUID.randomUUID();

        VocabularySense cachedSense = VocabularySense.builder()
                .id(cachedSenseId)
                .vocabulary(vocab)
                .partOfSpeech("adjective")
                .cefrLevel("B2")
                .definitionEn("able to recover quickly")
                .build();

        VocabularySense newSense = VocabularySense.builder()
                .id(newSenseId)
                .vocabulary(vocab)
                .partOfSpeech("adjective")
                .cefrLevel("C1")
                .definitionEn("able to return to original shape")
                .build();

        Idiom idiom = Idiom.builder()
                .id(idiomId)
                .vocabulary(vocab)
                .phrase("bounce back resiliently")
                .definition("recover from setback")
                .build();

        VocabularyList list = VocabularyList.builder()
                .id(listId)
                .owner(owner)
                .title("Test List")
                .status(VocabularyListStatus.DRAFT)
                .build();

        VocabularyListItem item = VocabularyListItem.builder()
                .id(UUID.randomUUID())
                .vocabularyList(list)
                .vocabulary(vocab)
                .rawWord("resilient")
                .normalizedWord("resilient")
                .build();
        item.setSelectedSenseIds(List.of(cachedSenseId, newSenseId));
        item.setSelectedIdiomIds(List.of(idiomId));

        when(listRepository.findById(listId)).thenReturn(Optional.of(list));
        when(listItemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(listId))
                .thenReturn(List.of(item));

        // cachedSense already has 5 questions -> should skip AI
        when(mcqRepository.countBySenseId(cachedSenseId)).thenReturn(5L);
        // newSense has 0 questions -> should call AI
        when(mcqRepository.countBySenseId(newSenseId)).thenReturn(0L);
        when(senseRepository.findById(newSenseId)).thenReturn(Optional.of(newSense));
        when(mcqRepository.findBySenseIdIn(List.of(newSenseId))).thenReturn(List.of());

        // idiom has 0 questions -> should call AI
        when(mcqRepository.countByIdiomId(idiomId)).thenReturn(0L);
        when(idiomRepository.findById(idiomId)).thenReturn(Optional.of(idiom));
        when(mcqRepository.findByIdiomIdIn(List.of(idiomId))).thenReturn(List.of());

        CustomListQuestionBatchService.AiMcqBatchResponse aiSenseResp = CustomListQuestionBatchService.AiMcqBatchResponse.builder()
                .questions(List.of(
                        CustomListQuestionBatchService.AiMcqItem.builder()
                                .questionCategory("CONTEXTUAL_CLOZE")
                                .grammarTopic("contextual_meaning")
                                .difficulty("C1")
                                .questionText("Rubber is a highly ____ material that returns to its shape.")
                                .options(List.of("resilient", "brittle", "fragile", "stiff"))
                                .correctAnswer("resilient")
                                .completedSentence("Rubber is a highly resilient material that returns to its shape.")
                                .explanationEn("Resilient means able to return to its original shape.")
                                .explanationVn("Resilient có nghĩa là đàn hồi về hình dạng ban đầu.")
                                .build()
                ))
                .build();

        CustomListQuestionBatchService.AiMcqBatchResponse aiIdiomResp = CustomListQuestionBatchService.AiMcqBatchResponse.builder()
                .questions(List.of(
                        CustomListQuestionBatchService.AiMcqItem.builder()
                                .questionCategory("IDIOM_PHRASAL_VERB")
                                .grammarTopic("idiom_usage")
                                .difficulty("B2")
                                .questionText("After the crisis, the company managed to ____.")
                                .options(List.of("bounce back resiliently", "fall behind", "give away", "turn down"))
                                .correctAnswer("bounce back resiliently")
                                .completedSentence("After the crisis, the company managed to bounce back resiliently.")
                                .explanationEn("Bounce back resiliently means recover quickly.")
                                .explanationVn("Cụm thành ngữ có nghĩa là phục hồi nhanh chóng.")
                                .build()
                ))
                .build();

        when(aiCoreClient.callAndValidate(
                anyString(),
                anyString(),
                eq(CustomListQuestionBatchService.AiMcqBatchResponse.class),
                any(Function.class)
        )).thenReturn(aiSenseResp, aiIdiomResp);

        when(mcqRepository.existsByContentHash(anyString())).thenReturn(false);

        batchService.runFinalizeBatchSync(listId, ownerId);

        verify(senseRepository, never()).findById(cachedSenseId);
        ArgumentCaptor<GrammarMultipleChoiceQuestion> questionCaptor = ArgumentCaptor.forClass(GrammarMultipleChoiceQuestion.class);
        verify(mcqRepository, org.mockito.Mockito.times(2)).save(questionCaptor.capture());

        List<GrammarMultipleChoiceQuestion> savedQuestions = questionCaptor.getAllValues();
        assertThat(savedQuestions).hasSize(2);
        assertThat(savedQuestions.get(0).getGenerationSource()).isEqualTo(QuestionGenerationSource.USER_LIST_FINALIZE_AI);
        assertThat(savedQuestions.get(0).getQuestionCategory()).isEqualTo(McqQuestionCategory.CONTEXTUAL_CLOZE);
        assertThat(savedQuestions.get(0).getSourceListId()).isEqualTo(listId);
        assertThat(savedQuestions.get(0).getCreatedByUserId()).isEqualTo(ownerId);

        assertThat(savedQuestions.get(1).getGenerationSource()).isEqualTo(QuestionGenerationSource.USER_LIST_FINALIZE_AI);
        assertThat(savedQuestions.get(1).getQuestionCategory()).isEqualTo(McqQuestionCategory.IDIOM_PHRASAL_VERB);
        assertThat(savedQuestions.get(1).getIdiom()).isEqualTo(idiom);

        assertThat(list.getStatus()).isEqualTo(VocabularyListStatus.ACTIVE);
        verify(notificationService).notifyUser(
                eq(owner),
                eq(UserNotificationType.LIST_QUESTIONS_READY),
                anyString(),
                anyString(),
                eq(listId),
                any()
        );
    }

    @Test
    void generateMoreQuestionsShouldUseMoreAiSourceFlagEvenWhenSenseAlreadyHasQuestions() {
        UUID listId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID senseId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("owner@test.com").fullName("Owner").build();

        Vocabulary vocab = Vocabulary.builder().id(UUID.randomUUID()).word("resilient").normalizedWord("resilient").build();
        VocabularySense sense = VocabularySense.builder()
                .id(senseId)
                .vocabulary(vocab)
                .partOfSpeech("adjective")
                .cefrLevel("B2")
                .definitionEn("able to recover quickly")
                .build();

        when(senseRepository.findById(senseId)).thenReturn(Optional.of(sense));
        when(mcqRepository.findBySenseIdIn(List.of(senseId))).thenReturn(List.of());
        when(mcqRepository.existsByContentHash(anyString())).thenReturn(false);

        CustomListQuestionBatchService.AiMcqBatchResponse aiResp = CustomListQuestionBatchService.AiMcqBatchResponse.builder()
                .questions(List.of(
                        CustomListQuestionBatchService.AiMcqItem.builder()
                                .questionCategory("WORD_FORM")
                                .grammarTopic("word_form")
                                .difficulty("B2")
                                .questionText("Her ____ helped her overcome the injury.")
                                .options(List.of("resilience", "resilient", "resiliently", "resiliency"))
                                .correctAnswer("resilience")
                                .completedSentence("Her resilience helped her overcome the injury.")
                                .explanationEn("After possessive determiner 'Her', a noun is required.")
                                .explanationVn("Sau tính từ sở hữu 'Her' cần một danh từ.")
                                .build()
                ))
                .build();

        when(aiCoreClient.callAndValidate(
                anyString(),
                anyString(),
                eq(CustomListQuestionBatchService.AiMcqBatchResponse.class),
                any(Function.class)
        )).thenReturn(aiResp);

        int generated = batchService.generateMoreForSenseSync(listId, ownerId, senseId, 3);
        assertThat(generated).isEqualTo(1);

        ArgumentCaptor<GrammarMultipleChoiceQuestion> captor = ArgumentCaptor.forClass(GrammarMultipleChoiceQuestion.class);
        verify(mcqRepository).save(captor.capture());
        assertThat(captor.getValue().getGenerationSource()).isEqualTo(QuestionGenerationSource.USER_LIST_MORE_AI);
        assertThat(captor.getValue().getQuestionCategory()).isEqualTo(McqQuestionCategory.WORD_FORM);
    }
}

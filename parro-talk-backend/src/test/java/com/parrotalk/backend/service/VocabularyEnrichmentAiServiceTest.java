package com.parrotalk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.UserNotificationType;
import com.parrotalk.backend.constant.VocabularyCrawlStatus;
import com.parrotalk.backend.constant.VocabularyDataSource;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyList;
import com.parrotalk.backend.entity.VocabularyListItem;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.VocabularyListItemRepository;
import com.parrotalk.backend.repository.VocabularyListRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VocabularyEnrichmentAiServiceTest {

    @Mock
    private AiCoreClient aiCoreClient;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private VocabularyListItemRepository listItemRepository;

    @Mock
    private VocabularyListRepository listRepository;

    @Mock
    private UserNotificationService userNotificationService;

    private ObjectMapper objectMapper;
    private VocabularyEnrichmentAiService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new VocabularyEnrichmentAiService(
                aiCoreClient,
                vocabularyRepository,
                topicRepository,
                listItemRepository,
                listRepository,
                userNotificationService,
                objectMapper
        );
    }

    @Test
    void shouldEnrichHybridCrawlResultAssignSourceFlagAndAutoSelectA1ToB2Senses() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        User owner = User.builder().id(userId).email("u@test.com").fullName("U").build();
        VocabularyList list = VocabularyList.builder().id(listId).owner(owner).title("My List").build();
        VocabularyListItem listItem = VocabularyListItem.builder()
                .id(itemId)
                .vocabularyList(list)
                .rawWord("resilient")
                .normalizedWord("resilient")
                .crawlStatus(VocabularyCrawlStatus.PENDING_CRAWL)
                .build();

        when(listItemRepository.findById(itemId)).thenReturn(Optional.of(listItem));
        when(vocabularyRepository.findByNormalizedWordIn(List.of("resilient"))).thenReturn(List.of());

        VocabularyEnrichmentAiService.AiEnrichmentResponse aiResp = VocabularyEnrichmentAiService.AiEnrichmentResponse.builder()
                .commonMeaningVn("kiên cường, đàn hồi")
                .senses(List.of(
                        VocabularyEnrichmentAiService.AiEnrichedSenseItem.builder()
                                .index(0)
                                .partOfSpeech("adjective")
                                .cefrLevel("B2")
                                .definitionEn("able to feel better quickly after something unpleasant")
                                .definitionVn("có khả năng hồi phục nhanh sau biến cố")
                                .exampleEn("Young people are amazingly resilient.")
                                .exampleVn("Người trẻ có khả năng phục hồi đáng kinh ngạc.")
                                .synonyms(List.of("tough", "adaptable", "buoyant"))
                                .antonyms(List.of("fragile", "vulnerable"))
                                .collocations(List.of("resilient economy"))
                                .topic("Personal qualities")
                                .build(),
                        VocabularyEnrichmentAiService.AiEnrichedSenseItem.builder()
                                .index(1)
                                .partOfSpeech("adjective")
                                .cefrLevel("C2")
                                .definitionEn("able to return to its original shape")
                                .definitionVn("đàn hồi về hình dạng ban đầu")
                                .exampleEn("resilient materials")
                                .exampleVn("vật liệu đàn hồi")
                                .synonyms(List.of("elastic"))
                                .antonyms(List.of("rigid"))
                                .build()
                ))
                .idioms(List.of(
                        VocabularyEnrichmentAiService.AiEnrichedPhraseItem.builder()
                                .phrase("bounce back resiliently")
                                .definitionEn("to recover quickly")
                                .definitionVn("vực dậy nhanh chóng")
                                .exampleEn("They bounced back resiliently.")
                                .exampleVn("Họ đã vực dậy nhanh chóng.")
                                .build()
                ))
                .build();

        when(aiCoreClient.callAndValidate(
                anyString(),
                anyString(),
                eq(VocabularyEnrichmentAiService.AiEnrichmentResponse.class),
                any(Function.class)
        )).thenReturn(aiResp);

        when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> {
            Vocabulary v = inv.getArgument(0);
            if (v.getId() == null) {
                v.setId(UUID.randomUUID());
            }
            for (VocabularySense s : v.getSenses()) {
                if (s.getId() == null) {
                    s.setId(UUID.randomUUID());
                }
            }
            v.getIdioms().forEach(i -> {
                if (i.getId() == null) {
                    i.setId(UUID.randomUUID());
                }
            });
            return v;
        });

        Map<String, Object> rawCrawlMessage = Map.of(
                "jobId", UUID.randomUUID().toString(),
                "listId", listId.toString(),
                "listItemId", itemId.toString(),
                "userId", userId.toString(),
                "word", "resilient",
                "status", "SUCCESS",
                "dataSource", "OXFORD_MW_HYBRID",
                "payload", Map.of(
                        "word", "resilient",
                        "wordForms", List.of("resilient", "resiliently", "resilience"),
                        "pronunciations", List.of(Map.of("accent", "UK", "ipa", "/rɪˈzɪliənt/", "audioUrl", "https://audio.mp3")),
                        "senses", List.of(
                                Map.of("partOfSpeech", "adjective", "cefrLevel", "B2", "definitionEn", "able to feel better quickly"),
                                Map.of("partOfSpeech", "adjective", "cefrLevel", "C2", "definitionEn", "able to return to shape")
                        ),
                        "mwSenseSynAntGroups", List.of(
                                Map.of("senseDefinition", "recover rapidly", "synonyms", List.of("adaptable"), "antonyms", List.of("fragile"))
                        ),
                        "idioms", List.of(
                                Map.of("phrase", "bounce back resiliently", "definitionEn", "to recover quickly")
                        ),
                        "phrasalVerbs", List.of()
                )
        );

        String messageJson = objectMapper.writeValueAsString(rawCrawlMessage);
        service.processCrawlResultMessage(messageJson);

        ArgumentCaptor<Vocabulary> vocabCaptor = ArgumentCaptor.forClass(Vocabulary.class);
        verify(vocabularyRepository).save(vocabCaptor.capture());
        Vocabulary savedVocab = vocabCaptor.getValue();

        assertThat(savedVocab.getDataSource()).isEqualTo(VocabularyDataSource.OXFORD_MW_HYBRID);
        assertThat(savedVocab.getCommonMeaningVn()).isEqualTo("kiên cường, đàn hồi");
        assertThat(savedVocab.getWordFormsJson()).contains("resiliently");
        assertThat(savedVocab.getSenses()).hasSize(2);
        assertThat(savedVocab.getSenses().get(0).getSynonyms()).extracting("synonymWord").contains("adaptable", "tough");
        assertThat(savedVocab.getIdioms()).hasSize(1);

        // Verify listItem updated & only B2 sense auto-selected (not C2)
        assertThat(listItem.getCrawlStatus()).isEqualTo(VocabularyCrawlStatus.COMPLETED);
        assertThat(listItem.getSelectedSenseIds()).containsExactly(savedVocab.getSenses().get(0).getId());
        assertThat(listItem.getSelectedIdiomIds()).containsExactly(savedVocab.getIdioms().get(0).getId());

        verify(userNotificationService).notifyUser(
                eq(owner),
                eq(UserNotificationType.VOCAB_CRAWL_COMPLETED),
                anyString(),
                anyString(),
                eq(listId),
                any()
        );
    }

    @Test
    void shouldFallbackToAiGeneratedWhenCrawlReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        User owner = User.builder().id(userId).email("u@test.com").fullName("U").build();
        VocabularyList list = VocabularyList.builder().id(listId).owner(owner).title("My List").build();
        VocabularyListItem listItem = VocabularyListItem.builder()
                .id(itemId)
                .vocabularyList(list)
                .rawWord("microplastic")
                .normalizedWord("microplastic")
                .crawlStatus(VocabularyCrawlStatus.PENDING_CRAWL)
                .build();

        when(listItemRepository.findById(itemId)).thenReturn(Optional.of(listItem));
        when(vocabularyRepository.findByNormalizedWordIn(List.of("microplastic"))).thenReturn(List.of());

        VocabularyEnrichmentAiService.AiEnrichmentResponse aiResp = VocabularyEnrichmentAiService.AiEnrichmentResponse.builder()
                .commonMeaningVn("vi nhựa")
                .senses(List.of(
                        VocabularyEnrichmentAiService.AiEnrichedSenseItem.builder()
                                .index(0)
                                .partOfSpeech("noun")
                                .cefrLevel("B2")
                                .definitionEn("extremely small pieces of plastic debris")
                                .definitionVn("các mảnh nhựa cực nhỏ")
                                .exampleEn("Microplastics pollute the ocean.")
                                .exampleVn("Vi nhựa gây ô nhiễm đại dương.")
                                .build()
                ))
                .build();

        when(aiCoreClient.callAndValidate(
                anyString(),
                anyString(),
                eq(VocabularyEnrichmentAiService.AiEnrichmentResponse.class),
                any(Function.class)
        )).thenReturn(aiResp);

        when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> {
            Vocabulary v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            v.getSenses().forEach(s -> s.setId(UUID.randomUUID()));
            return v;
        });

        Map<String, Object> rawCrawlMessage = Map.of(
                "jobId", UUID.randomUUID().toString(),
                "listId", listId.toString(),
                "listItemId", itemId.toString(),
                "userId", userId.toString(),
                "word", "microplastic",
                "status", "NOT_FOUND",
                "dataSource", "NOT_FOUND",
                "payload", Map.of("word", "microplastic")
        );

        service.processCrawlResultMessage(objectMapper.writeValueAsString(rawCrawlMessage));

        ArgumentCaptor<Vocabulary> vocabCaptor = ArgumentCaptor.forClass(Vocabulary.class);
        verify(vocabularyRepository).save(vocabCaptor.capture());
        Vocabulary saved = vocabCaptor.getValue();

        assertThat(saved.getDataSource()).isEqualTo(VocabularyDataSource.AI_GENERATED);
        assertThat(saved.getSenses().get(0).getDataSource()).isEqualTo(VocabularyDataSource.AI_GENERATED);
        assertThat(saved.getSenses().get(0).getIsAiCefr()).isTrue();
    }
}


package com.parrotalk.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.McqQuestionCategory;
import com.parrotalk.backend.constant.QuestionGenerationSource;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.VocabularyCrawlStatus;
import com.parrotalk.backend.constant.VocabularyDataSource;
import com.parrotalk.backend.constant.VocabularyListStatus;
import com.parrotalk.backend.constant.VocabularyListVisibility;
import com.parrotalk.backend.entity.GrammarMultipleChoiceQuestion;
import com.parrotalk.backend.entity.Idiom;
import com.parrotalk.backend.entity.PhrasalVerb;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyList;
import com.parrotalk.backend.entity.VocabularyListItem;
import com.parrotalk.backend.entity.VocabularyListShare;
import com.parrotalk.backend.entity.VocabularySense;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomVocabularyListRepositoryTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VocabularyListRepository vocabularyListRepository;

    @Autowired
    private VocabularyListItemRepository vocabularyListItemRepository;

    @Autowired
    private VocabularyListShareRepository vocabularyListShareRepository;

    @Autowired
    private GrammarMultipleChoiceQuestionRepository mcqRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistSourceFlagsCustomListItemsSharesAndMcqQuestions() {
        User owner = User.builder()
                .email("owner-list@test.com")
                .displayUsername("ownerlist")
                .fullName("Owner User")
                .role(Role.USER)
                .password("hashed")
                .build();
        owner = userRepository.save(owner);

        User viewer = User.builder()
                .email("viewer-list@test.com")
                .displayUsername("viewerlist")
                .fullName("Viewer User")
                .role(Role.USER)
                .password("hashed")
                .build();
        viewer = userRepository.save(viewer);

        Vocabulary vocab = Vocabulary.builder()
                .word("resilient")
                .normalizedWord("resilient")
                .commonMeaningVn("kiên cường, đàn hồi")
                .dataSource(VocabularyDataSource.OXFORD_MW_HYBRID)
                .wordFormsJson("[\"resilient\",\"resiliently\",\"resilience\"]")
                .build();

        VocabularySense sense = VocabularySense.builder()
                .vocabulary(vocab)
                .partOfSpeech("adjective")
                .cefrLevel("B2")
                .definitionEn("able to feel better quickly after something unpleasant")
                .definitionVn("có khả năng phục hồi nhanh")
                .dataSource(VocabularyDataSource.OXFORD_MW_HYBRID)
                .build();
        vocab.getSenses().add(sense);

        Idiom idiom = Idiom.builder()
                .vocabulary(vocab)
                .phrase("bounce back resiliently")
                .definition("recover quickly")
                .definitionVn("hồi phục nhanh chóng")
                .dataSource(VocabularyDataSource.OXFORD_CRAWLER)
                .build();
        vocab.getIdioms().add(idiom);

        PhrasalVerb pv = PhrasalVerb.builder()
                .vocabulary(vocab)
                .phrase("resilient up")
                .definition("strengthen up")
                .definitionVn("vững vàng lên")
                .dataSource(VocabularyDataSource.MERRIAM_WEBSTER_API)
                .build();
        vocab.getPhrasalVerbs().add(pv);

        vocab = vocabularyRepository.save(vocab);
        UUID senseId = vocab.getSenses().get(0).getId();
        UUID idiomId = vocab.getIdioms().get(0).getId();
        UUID pvId = vocab.getPhrasalVerbs().get(0).getId();

        VocabularyList list = VocabularyList.builder()
                .owner(owner)
                .title("IELTS Environment & Psychology")
                .description("My personal curated words")
                .visibility(VocabularyListVisibility.PRIVATE)
                .status(VocabularyListStatus.DRAFT)
                .shareToken("tok_test_123456")
                .build();
        list = vocabularyListRepository.save(list);

        VocabularyListItem item = VocabularyListItem.builder()
                .vocabularyList(list)
                .vocabulary(vocab)
                .rawWord("resilient")
                .normalizedWord("resilient")
                .crawlStatus(VocabularyCrawlStatus.COMPLETED)
                .build();
        item.setSelectedSenseIds(List.of(senseId));
        item.setSelectedIdiomIds(List.of(idiomId));
        item.setSelectedPhrasalVerbIds(List.of(pvId));
        item = vocabularyListItemRepository.save(item);

        VocabularyListShare share = VocabularyListShare.builder()
                .vocabularyList(list)
                .sharedWithUser(viewer)
                .grantedByUser(owner)
                .role("VIEWER")
                .build();
        vocabularyListShareRepository.save(share);

        GrammarMultipleChoiceQuestion senseQ = GrammarMultipleChoiceQuestion.builder()
                .vocabulary(vocab)
                .sense(vocab.getSenses().get(0))
                .grammarTopic("synonym_selection")
                .questionCategory(McqQuestionCategory.SYNONYM_IN_CONTEXT)
                .generationSource(QuestionGenerationSource.USER_LIST_FINALIZE_AI)
                .difficulty("B2")
                .questionText("Which word is CLOSEST in meaning to **resilient** in: 'Children are very **resilient**.'?")
                .optionsJson("[\"tough\",\"fragile\",\"rigid\",\"weak\"]")
                .correctIndex((short) 0)
                .correctAnswer("tough")
                .completedSentence("Children are very resilient (tough).")
                .explanationEn("Resilient means tough and able to recover quickly.")
                .explanationVn("Resilient đồng nghĩa với tough trong ngữ cảnh này.")
                .contentHash("hash_resilient_syn_001")
                .sourceListId(list.getId())
                .createdByUserId(owner.getId())
                .build();
        mcqRepository.save(senseQ);

        GrammarMultipleChoiceQuestion idiomQ = GrammarMultipleChoiceQuestion.builder()
                .vocabulary(vocab)
                .idiom(vocab.getIdioms().get(0))
                .grammarTopic("idiom_application")
                .questionCategory(McqQuestionCategory.IDIOM_PHRASAL_VERB)
                .generationSource(QuestionGenerationSource.USER_LIST_MORE_AI)
                .difficulty("C1")
                .questionText("After losing the first match, the team managed to ____.")
                .optionsJson("[\"bounce back resiliently\",\"give in\",\"fall apart\",\"drop out\"]")
                .correctIndex((short) 0)
                .correctAnswer("bounce back resiliently")
                .completedSentence("After losing the first match, the team managed to bounce back resiliently.")
                .explanationEn("Bounce back resiliently means recover quickly from a setback.")
                .explanationVn("Cụm thành ngữ mang nghĩa vực dậy nhanh chóng sau thất bại.")
                .contentHash("hash_resilient_idm_001")
                .sourceListId(list.getId())
                .createdByUserId(owner.getId())
                .build();
        mcqRepository.save(idiomQ);

        entityManager.flush();
        entityManager.clear();

        Vocabulary reloadedVocab = vocabularyRepository.findById(vocab.getId()).orElseThrow();
        assertThat(reloadedVocab.getDataSource()).isEqualTo(VocabularyDataSource.OXFORD_MW_HYBRID);
        assertThat(reloadedVocab.getSenses().get(0).getDataSource()).isEqualTo(VocabularyDataSource.OXFORD_MW_HYBRID);
        assertThat(reloadedVocab.getIdioms().get(0).getDataSource()).isEqualTo(VocabularyDataSource.OXFORD_CRAWLER);
        assertThat(reloadedVocab.getPhrasalVerbs().get(0).getDataSource()).isEqualTo(VocabularyDataSource.MERRIAM_WEBSTER_API);

        VocabularyListItem reloadedItem = vocabularyListItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloadedItem.getSelectedSenseIds()).containsExactly(senseId);
        assertThat(reloadedItem.getSelectedIdiomIds()).containsExactly(idiomId);
        assertThat(reloadedItem.getSelectedPhrasalVerbIds()).containsExactly(pvId);

        List<GrammarMultipleChoiceQuestion> senseQuestions = mcqRepository.findBySenseIdIn(List.of(senseId));
        assertThat(senseQuestions).hasSize(1);
        assertThat(senseQuestions.get(0).getGenerationSource()).isEqualTo(QuestionGenerationSource.USER_LIST_FINALIZE_AI);
        assertThat(senseQuestions.get(0).getQuestionCategory()).isEqualTo(McqQuestionCategory.SYNONYM_IN_CONTEXT);

        List<GrammarMultipleChoiceQuestion> idiomQuestions = mcqRepository.findByIdiomIdIn(List.of(idiomId));
        assertThat(idiomQuestions).hasSize(1);
        assertThat(idiomQuestions.get(0).getGenerationSource()).isEqualTo(QuestionGenerationSource.USER_LIST_MORE_AI);
    }
}

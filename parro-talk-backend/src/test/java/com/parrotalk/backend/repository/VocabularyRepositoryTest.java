package com.parrotalk.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyDetailDto;
import com.parrotalk.backend.entity.Idiom;
import com.parrotalk.backend.entity.PhrasalVerb;
import com.parrotalk.backend.entity.SenseAntonym;
import com.parrotalk.backend.entity.SenseCollocation;
import com.parrotalk.backend.entity.SenseSynonym;
import com.parrotalk.backend.entity.Topic;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyPronunciation;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.service.admin.AdminCoreVocabularyService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class VocabularyRepositoryTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private AdminCoreVocabularyService adminCoreVocabularyService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testFindByIdWithGraphAndServiceEndToEnd() {
        Topic topic = new Topic();
        topic.setName("Education");
        topic = topicRepository.save(topic);

        Vocabulary vocab = new Vocabulary();
        vocab.setWord("testword");
        vocab.setNormalizedWord("testword");
        vocab.setCommonMeaningVn("Từ thử nghiệm");

        VocabularyPronunciation pron = new VocabularyPronunciation();
        pron.setVocabulary(vocab);
        pron.setAccent("US");
        pron.setIpa("/test/");
        vocab.getPronunciations().add(pron);

        VocabularySense sense = new VocabularySense();
        sense.setVocabulary(vocab);
        sense.setPartOfSpeech("noun");
        sense.setCefrLevel("B1");
        sense.setDefinitionEn("A test definition");
        sense.setDefinitionVn("Một định nghĩa thử nghiệm");
        sense.setTopic(topic);

        SenseSynonym syn = new SenseSynonym();
        syn.setSense(sense);
        syn.setSynonymWord("exam");
        sense.getSynonyms().add(syn);

        SenseAntonym ant = new SenseAntonym();
        ant.setSense(sense);
        ant.setAntonymWord("practice");
        sense.getAntonyms().add(ant);

        SenseCollocation col = new SenseCollocation();
        col.setSense(sense);
        col.setCollocationWord("test score");
        sense.getCollocations().add(col);

        vocab.getSenses().add(sense);

        Idiom idiom = new Idiom();
        idiom.setVocabulary(vocab);
        idiom.setPhrase("put to the test");
        idiom.setDefinition("try something out");
        vocab.getIdioms().add(idiom);

        PhrasalVerb pv = new PhrasalVerb();
        pv.setVocabulary(vocab);
        pv.setPhrase("test out");
        pv.setDefinition("experiment with");
        vocab.getPhrasalVerbs().add(pv);

        vocab = vocabularyRepository.save(vocab);

        entityManager.flush();
        entityManager.clear();

        // 1. Verify findByIdWithGraph does NOT throw MultipleBagFetchException
        Optional<Vocabulary> foundOpt = vocabularyRepository.findByIdWithGraph(vocab.getId());
        assertThat(foundOpt).isPresent();

        // 2. Verify adminCoreVocabularyService.getVocabulary works end-to-end
        AdminVocabularyDetailDto detail = adminCoreVocabularyService.getVocabulary(vocab.getId());
        assertThat(detail).isNotNull();
        assertThat(detail.getWord()).isEqualTo("testword");
        assertThat(detail.getSenses().get(0).getTopics()).contains("Education");
        assertThat(detail.getPronunciations()).hasSize(1);
        assertThat(detail.getSenses()).hasSize(1);
        assertThat(detail.getSenses().get(0).getSynonyms()).contains("exam");
        assertThat(detail.getSenses().get(0).getAntonyms()).contains("practice");
        assertThat(detail.getSenses().get(0).getCollocations()).contains("test score");
        assertThat(detail.getIdioms()).hasSize(1);
        assertThat(detail.getPhrasalVerbs()).hasSize(1);
    }
}

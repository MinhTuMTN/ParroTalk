package com.parrotalk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parrotalk.backend.constant.VocabularyCrawlStatus;
import com.parrotalk.backend.constant.VocabularyListVisibility;
import com.parrotalk.backend.dto.vocablist.AddVocabularyListItemRequest;
import com.parrotalk.backend.dto.vocablist.CreateVocabularyListRequest;
import com.parrotalk.backend.dto.vocablist.SubmitCustomListAnswerRequest;
import com.parrotalk.backend.dto.vocablist.VocabularyListDetailDto;
import com.parrotalk.backend.dto.vocablist.VocabularyListItemDto;
import com.parrotalk.backend.entity.GrammarMultipleChoiceQuestion;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.entity.VocabularyList;
import com.parrotalk.backend.entity.VocabularyListItem;
import com.parrotalk.backend.entity.VocabularySense;
import com.parrotalk.backend.repository.GrammarMultipleChoiceQuestionRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.UserVocabularyListProgressRepository;
import com.parrotalk.backend.repository.VocabularyListItemRepository;
import com.parrotalk.backend.repository.VocabularyListRepository;
import com.parrotalk.backend.repository.VocabularyListShareRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CustomVocabularyListServiceTest {

    @Mock
    private VocabularyListRepository listRepository;

    @Mock
    private VocabularyListItemRepository itemRepository;

    @Mock
    private VocabularyListShareRepository shareRepository;

    @Mock
    private UserVocabularyListProgressRepository progressRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private GrammarMultipleChoiceQuestionRepository mcqRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VocabCrawlTaskProducer crawlTaskProducer;

    @Mock
    private CustomListQuestionBatchService questionBatchService;

    @Mock
    private UserNotificationService notificationService;

    private CustomVocabularyListService service;

    @BeforeEach
    void setUp() {
        service = new CustomVocabularyListService(
                listRepository,
                itemRepository,
                shareRepository,
                progressRepository,
                vocabularyRepository,
                mcqRepository,
                userRepository,
                crawlTaskProducer,
                questionBatchService,
                notificationService
        );
    }

    @Test
    void shouldRejectUnauthorizedUserAccessingPrivateList() {
        UUID listId = UUID.randomUUID();
        User owner = User.builder().id(UUID.randomUUID()).email("owner@test.com").fullName("Owner").build();
        User stranger = User.builder().id(UUID.randomUUID()).email("stranger@test.com").fullName("Stranger").build();

        VocabularyList list = VocabularyList.builder()
                .id(listId)
                .owner(owner)
                .title("Secret List")
                .visibility(VocabularyListVisibility.PRIVATE)
                .shareToken("secret_token")
                .build();

        when(listRepository.findById(listId)).thenReturn(Optional.of(list));
        when(shareRepository.existsByVocabularyListIdAndSharedWithUserId(listId, stranger.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getListDetail(stranger, listId, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void shouldAllowAccessWithValidShareTokenAndSupportCloning() {
        UUID listId = UUID.randomUUID();
        UUID senseId = UUID.randomUUID();
        User owner = User.builder().id(UUID.randomUUID()).email("owner@test.com").fullName("Owner").build();
        User viewer = User.builder().id(UUID.randomUUID()).email("viewer@test.com").fullName("Viewer").build();

        Vocabulary vocab = Vocabulary.builder().id(UUID.randomUUID()).word("resilient").normalizedWord("resilient").build();
        VocabularySense sense = VocabularySense.builder().id(senseId).vocabulary(vocab).cefrLevel("B2").definitionEn("def").build();
        vocab.getSenses().add(sense);

        VocabularyList list = VocabularyList.builder()
                .id(listId)
                .owner(owner)
                .title("Shared IELTS List")
                .visibility(VocabularyListVisibility.PRIVATE)
                .shareToken("valid_share_token")
                .build();

        VocabularyListItem item = VocabularyListItem.builder()
                .id(UUID.randomUUID())
                .vocabularyList(list)
                .vocabulary(vocab)
                .rawWord("resilient")
                .normalizedWord("resilient")
                .crawlStatus(VocabularyCrawlStatus.COMPLETED)
                .build();
        item.setSelectedSenseIds(List.of(senseId));

        when(listRepository.findById(listId)).thenReturn(Optional.of(list));
        when(itemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(listId)).thenReturn(List.of(item));
        when(shareRepository.findByVocabularyListIdOrderByCreatedAtDesc(listId)).thenReturn(List.of());
        when(progressRepository.findByUserIdAndVocabularyListId(viewer.getId(), listId)).thenReturn(List.of());

        VocabularyListDetailDto detail = service.getListDetail(viewer, listId, "valid_share_token");
        assertThat(detail.title()).isEqualTo("Shared IELTS List");
        assertThat(detail.isOwner()).isFalse();

        when(listRepository.save(any(VocabularyList.class))).thenAnswer(inv -> {
            VocabularyList cloned = inv.getArgument(0);
            cloned.setId(UUID.randomUUID());
            return cloned;
        });

        VocabularyListDetailDto clonedDetail = service.cloneList(viewer, listId, "valid_share_token");
        assertThat(clonedDetail.isOwner()).isTrue();
        assertThat(clonedDetail.clonedFromListId()).isEqualTo(listId);
        verify(itemRepository).saveAll(any());
    }

    @Test
    void addWordsShouldDispatchCrawlJobWhenExternalCrawlOptionSelectedOrWordMissingInDb() {
        UUID listId = UUID.randomUUID();
        User owner = User.builder().id(UUID.randomUUID()).email("owner@test.com").fullName("Owner").build();
        VocabularyList list = VocabularyList.builder().id(listId).owner(owner).title("List").build();

        when(listRepository.findById(listId)).thenReturn(Optional.of(list));
        when(itemRepository.findByVocabularyListIdAndNormalizedWord(listId, " ephemeral ".trim())).thenReturn(Optional.empty());
        when(itemRepository.save(any(VocabularyListItem.class))).thenAnswer(inv -> {
            VocabularyListItem it = inv.getArgument(0);
            it.setId(UUID.randomUUID());
            return it;
        });

        AddVocabularyListItemRequest req = new AddVocabularyListItemRequest(
                List.of("ephemeral"),
                "EXTERNAL_CRAWL",
                "Need Oxford + MW data"
        );

        service.addWordsToList(owner, listId, req);

        verify(crawlTaskProducer).sendCrawlTask(eq(listId), any(UUID.class), eq(owner.getId()), eq("ephemeral"));
    }

    @Test
    void searchWords_shouldReturnMatchingVocabularyDtos() {
        Vocabulary vocab = Vocabulary.builder()
                .id(UUID.randomUUID())
                .word("resilient")
                .normalizedWord("resilient")
                .build();
        when(vocabularyRepository.findByNormalizedWordStartingWith(eq("resil"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(vocab)));

        List<VocabularyListItemDto> results = service.searchWords("resil");
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).normalizedWord()).isEqualTo("resilient");
    }

    @Test
    void addWordsToList_withSingleRawWord_shouldSucceed() {
        UUID listId = UUID.randomUUID();
        User owner = User.builder().id(UUID.randomUUID()).email("owner@test.com").fullName("Owner").build();
        VocabularyList list = VocabularyList.builder().id(listId).owner(owner).title("List").build();

        when(listRepository.findById(listId)).thenReturn(Optional.of(list));
        when(itemRepository.findByVocabularyListIdAndNormalizedWord(listId, "serendipity")).thenReturn(Optional.empty());
        when(itemRepository.save(any(VocabularyListItem.class))).thenAnswer(inv -> {
            VocabularyListItem it = inv.getArgument(0);
            it.setId(UUID.randomUUID());
            return it;
        });

        AddVocabularyListItemRequest req = new AddVocabularyListItemRequest(
                null,
                "serendipity",
                null,
                "DB_EXISTING",
                "My note"
        );

        List<VocabularyListItemDto> dtos = service.addWordsToList(owner, listId, req);
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).normalizedWord()).isEqualTo("serendipity");
    }

    @Test
    void recordAnswerProgress_withQuestionIdOnly_shouldFindItemAndRecordProgress() {
        UUID listId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).email("learner@test.com").build();
        VocabularyList list = VocabularyList.builder().id(listId).build();

        UUID questionId = UUID.randomUUID();
        UUID senseId = UUID.randomUUID();
        GrammarMultipleChoiceQuestion q = GrammarMultipleChoiceQuestion.builder()
                .id(questionId)
                .sense(VocabularySense.builder().id(senseId).build())
                .build();

        VocabularyListItem item = VocabularyListItem.builder()
                .id(UUID.randomUUID())
                .vocabularyList(list)
                .build();
        item.setSelectedSenseIds(List.of(senseId));

        when(listRepository.findById(listId)).thenReturn(Optional.of(list));
        when(mcqRepository.findById(questionId)).thenReturn(Optional.of(q));
        when(itemRepository.findByVocabularyListIdOrderByDisplayOrderAscCreatedAtAsc(listId))
                .thenReturn(List.of(item));
        when(progressRepository.findByUserIdAndListItemId(user.getId(), item.getId()))
                .thenReturn(Optional.empty());

        SubmitCustomListAnswerRequest req = new SubmitCustomListAnswerRequest(null, questionId, true);
        service.recordAnswerProgress(user, listId, req);

        verify(progressRepository).save(any());
    }
}


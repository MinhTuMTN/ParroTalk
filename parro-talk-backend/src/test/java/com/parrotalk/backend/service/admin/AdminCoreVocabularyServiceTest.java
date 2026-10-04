package com.parrotalk.backend.service.admin;

import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyDetailDto;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyListDto;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyRequestDto;
import com.parrotalk.backend.entity.*;
import com.parrotalk.backend.repository.TopicRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminCoreVocabularyServiceTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private AdminCoreVocabularyService vocabularyService;

    private Vocabulary vocab;
    private UUID vocabId;

    @BeforeEach
    void setUp() {
        vocabId = UUID.randomUUID();
        vocab = new Vocabulary();
        vocab.setId(vocabId);
        vocab.setWord("test");
        vocab.setNormalizedWord("test");
        vocab.setCommonMeaningVn("kiem tra");
        vocab.setSenses(new ArrayList<>());
        vocab.setPronunciations(new ArrayList<>());
        vocab.setIdioms(new ArrayList<>());
        vocab.setPhrasalVerbs(new ArrayList<>());
    }

    @Test
    void getVocabulary_Found_ReturnsDto() {
        when(vocabularyRepository.findByIdWithGraph(vocabId)).thenReturn(Optional.of(vocab));
        
        AdminVocabularyDetailDto result = vocabularyService.getVocabulary(vocabId);
        
        assertNotNull(result);
        assertEquals("test", result.getWord());
        verify(vocabularyRepository, times(1)).findByIdWithGraph(vocabId);
    }

    @Test
    void getVocabulary_NotFound_ThrowsException() {
        when(vocabularyRepository.findByIdWithGraph(vocabId)).thenReturn(Optional.empty());
        
        assertThrows(ResponseStatusException.class, () -> vocabularyService.getVocabulary(vocabId));
    }

    @Test
    void getVocabularies_ReturnsPageOfDtos() {
        Page<Vocabulary> page = new PageImpl<>(List.of(vocab));
        when(vocabularyRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Vocabulary>>any(), any(Pageable.class))).thenReturn(page);
        
        Page<AdminVocabularyListDto> result = vocabularyService.getVocabularies("test", null, null, null, PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test", result.getContent().get(0).getWord());
    }

    @Test
    void createVocabulary_Success() {
        AdminVocabularyRequestDto request = new AdminVocabularyRequestDto();
        request.setWord("newWord");
        request.setCommonMeaningVn("nghia");
        
        when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(invocation -> {
            Vocabulary saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AdminVocabularyDetailDto result = vocabularyService.createVocabulary(request);
        
        assertNotNull(result);
        assertEquals("newWord", result.getWord());
        verify(vocabularyRepository, times(1)).save(any(Vocabulary.class));
    }

    @Test
    void updateVocabulary_Success() {
        AdminVocabularyRequestDto request = new AdminVocabularyRequestDto();
        request.setWord("updatedWord");
        request.setCommonMeaningVn("nghia moi");
        
        when(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab));
        when(vocabularyRepository.save(any(Vocabulary.class))).thenReturn(vocab);

        AdminVocabularyDetailDto result = vocabularyService.updateVocabulary(vocabId, request);
        
        assertNotNull(result);
        assertEquals("updatedWord", result.getWord());
        assertEquals("updatedword", vocab.getNormalizedWord());
        verify(vocabularyRepository, times(1)).save(vocab);
    }

    @Test
    void deleteVocabulary_Success() {
        when(vocabularyRepository.existsById(vocabId)).thenReturn(true);
        doNothing().when(vocabularyRepository).deleteById(vocabId);

        assertDoesNotThrow(() -> vocabularyService.deleteVocabulary(vocabId));
        verify(vocabularyRepository, times(1)).deleteById(vocabId);
    }

    @Test
    void deleteVocabulary_NotFound_ThrowsException() {
        when(vocabularyRepository.existsById(vocabId)).thenReturn(false);
        
        assertThrows(ResponseStatusException.class, () -> vocabularyService.deleteVocabulary(vocabId));
        verify(vocabularyRepository, never()).deleteById(any());
    }
}

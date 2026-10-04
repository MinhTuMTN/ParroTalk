package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.vocabulary.VocabularyDetailDTO;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.repository.VocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VocabularyServiceTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @InjectMocks
    private VocabularyService vocabularyService;

    @Test
    void getVocabularyByWord_Found_ReturnsDTO() {
        Vocabulary vocab = new Vocabulary();
        vocab.setWord("issue");
        vocab.setNormalizedWord("issue");

        when(vocabularyRepository.findByNormalizedWord("issue"))
                .thenReturn(Optional.of(vocab));

        VocabularyDetailDTO result = vocabularyService.getVocabularyByWord("Issue ");

        assertNotNull(result);
        assertEquals("issue", result.getWord());
    }

    @Test
    void getVocabularyByWord_NotFound_ReturnsNull() {
        when(vocabularyRepository.findByNormalizedWord(anyString()))
                .thenReturn(Optional.empty());

        VocabularyDetailDTO result = vocabularyService.getVocabularyByWord("unknown");

        assertNull(result);
    }
}

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(vocabularyRepository.findFirstByWordForm(anyString()))
                .thenReturn(Optional.empty());

        VocabularyDetailDTO result = vocabularyService.getVocabularyByWord("unknown");

        assertNull(result);
    }

    @Test
    void getVocabularyByWord_WordFormFallback_ReturnsMatchedFrom() {
        Vocabulary story = new Vocabulary();
        story.setWord("story");
        story.setNormalizedWord("story");

        when(vocabularyRepository.findByNormalizedWord("stories"))
                .thenReturn(Optional.empty());
        when(vocabularyRepository.findFirstByWordForm("stories"))
                .thenReturn(Optional.of(story));

        VocabularyDetailDTO result = vocabularyService.getVocabularyByWord("stories");

        assertNotNull(result);
        assertEquals("story", result.getWord());
        assertEquals("stories", result.getMatchedFrom());
    }

    @Test
    void getVocabularyByWord_ExactMatch_MatchedFromIsNullAndDoesNotCallWordFormFallback() {
        Vocabulary story = new Vocabulary();
        story.setWord("story");
        story.setNormalizedWord("story");

        when(vocabularyRepository.findByNormalizedWord("story"))
                .thenReturn(Optional.of(story));

        VocabularyDetailDTO result = vocabularyService.getVocabularyByWord("story");

        assertNotNull(result);
        assertEquals("story", result.getWord());
        assertNull(result.getMatchedFrom());
        verify(vocabularyRepository, never()).findFirstByWordForm(anyString());
    }

    @Test
    void getVocabularyByWord_PossessiveAndPunctuation_ResolvesCleanly() {
        Vocabulary story = new Vocabulary();
        story.setWord("story");
        story.setNormalizedWord("story");

        when(vocabularyRepository.findByNormalizedWord("story's"))
                .thenReturn(Optional.empty());
        when(vocabularyRepository.findByNormalizedWord("story"))
                .thenReturn(Optional.of(story));

        VocabularyDetailDTO possessiveResult = vocabularyService.getVocabularyByWord("story's");
        assertNotNull(possessiveResult);
        assertEquals("story", possessiveResult.getWord());
        assertNull(possessiveResult.getMatchedFrom());

        when(vocabularyRepository.findByNormalizedWord("stories"))
                .thenReturn(Optional.empty());
        when(vocabularyRepository.findFirstByWordForm("stories"))
                .thenReturn(Optional.of(story));

        VocabularyDetailDTO punctuatedResult = vocabularyService.getVocabularyByWord("stories,");
        assertNotNull(punctuatedResult);
        assertEquals("story", punctuatedResult.getWord());
        assertEquals("stories", punctuatedResult.getMatchedFrom());
    }

    @Test
    void getVocabularyByWord_BlankInput_ReturnsNullWithoutQuerying() {
        VocabularyDetailDTO result = vocabularyService.getVocabularyByWord("   ");

        assertNull(result);
        verify(vocabularyRepository, never()).findByNormalizedWord(anyString());
        verify(vocabularyRepository, never()).findFirstByWordForm(anyString());
    }

    @Test
    void getVocabularyByWord_PossessiveOfWordForm_StripsPossessiveAndFallsBackToWordForm() {
        Vocabulary child = new Vocabulary();
        child.setWord("child");
        child.setNormalizedWord("child");

        when(vocabularyRepository.findByNormalizedWord("children's"))
                .thenReturn(Optional.empty());
        when(vocabularyRepository.findByNormalizedWord("children"))
                .thenReturn(Optional.empty());
        when(vocabularyRepository.findFirstByWordForm("children's"))
                .thenReturn(Optional.empty());
        when(vocabularyRepository.findFirstByWordForm("children"))
                .thenReturn(Optional.of(child));

        VocabularyDetailDTO result = vocabularyService.getVocabularyByWord("children's");

        assertNotNull(result);
        assertEquals("child", result.getWord());
        assertEquals("children", result.getMatchedFrom());
    }
}

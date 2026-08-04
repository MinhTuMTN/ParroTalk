package com.parrotalk.backend.service.cms;

import com.parrotalk.backend.dto.admin.AdminVocabularyRequestDto;
import com.parrotalk.backend.dto.admin.AdminVocabularyResponseDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.cms.AdminVocabulary;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.TagRepository;
import com.parrotalk.backend.repository.cms.AdminVocabularyRepository;
import com.parrotalk.backend.repository.cms.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVocabularyServiceTest {

    @Mock
    private AdminVocabularyRepository vocabularyRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminVocabularyService vocabularyService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
    }

    @Test
    void testCreateVocabulary_Success() {
        AdminVocabularyRequestDto req = new AdminVocabularyRequestDto();
        req.setWord("apple");
        req.setCefrLevel("A1");
        req.setPartOfSpeech("noun");

        when(vocabularyRepository.existsByWordAndPartOfSpeechAndCefrLevel("apple", "noun", "A1")).thenReturn(false);
        
        AdminVocabulary savedEntity = new AdminVocabulary();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setWord("apple");
        savedEntity.setCefrLevel("A1");
        savedEntity.setPartOfSpeech("noun");
        
        when(vocabularyRepository.save(any(AdminVocabulary.class))).thenReturn(savedEntity);

        AdminVocabularyResponseDto res = vocabularyService.createVocabulary(req, mockUser);

        assertNotNull(res);
        assertEquals("apple", res.getWord());
        assertEquals("A1", res.getCefrLevel());
    }

    @Test
    void testCreateVocabulary_DuplicateThrowsConflict() {
        AdminVocabularyRequestDto req = new AdminVocabularyRequestDto();
        req.setWord("apple");
        req.setCefrLevel("A1");
        req.setPartOfSpeech("noun");

        when(vocabularyRepository.existsByWordAndPartOfSpeechAndCefrLevel("apple", "noun", "A1")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> vocabularyService.createVocabulary(req, mockUser));
    }
}

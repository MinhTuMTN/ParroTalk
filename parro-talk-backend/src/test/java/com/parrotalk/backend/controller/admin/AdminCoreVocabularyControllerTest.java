package com.parrotalk.backend.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyRequestDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Vocabulary;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.repository.VocabularyRepository;
import com.parrotalk.backend.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminCoreVocabularyControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private Vocabulary testVocab;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        User adminUser = userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .fullName("Admin User")
                .password("password")
                .role(com.parrotalk.backend.constant.Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build());
        adminToken = jwtUtils.generateToken(adminUser);

        User normalUser = userRepository.save(User.builder()
                .email("user-" + UUID.randomUUID() + "@example.com")
                .fullName("Normal User")
                .password("password")
                .role(com.parrotalk.backend.constant.Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());
        userToken = jwtUtils.generateToken(normalUser);

        testVocab = new Vocabulary();
        testVocab.setWord("IntegrationTest");
        testVocab.setNormalizedWord("integrationtest");
        testVocab = vocabularyRepository.save(testVocab);
    }

    @Test
    void getVocabularies_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/admin/vocabularies")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content").isArray());
    }

    @Test
    void getVocabularies_User_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/vocabularies")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void createVocabulary_Admin_Success() throws Exception {
        AdminVocabularyRequestDto request = new AdminVocabularyRequestDto();
        request.setWord("NewWord");
        request.setCommonMeaningVn("Tu moi");

        mockMvc.perform(post("/api/admin/vocabularies")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.word").value("NewWord"));
    }

    @Test
    void deleteVocabulary_Admin_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/vocabularies/" + testVocab.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
                
        assertFalse(vocabularyRepository.existsById(testVocab.getId()));
    }
}

package com.parrotalk.backend.controller;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.constant.WritingMode;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.writing.GrammarValidationResultDto;
import com.parrotalk.backend.dto.writing.WritingAnalyzeRequestDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionSummaryDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.GlobalExceptionHandler;
import com.parrotalk.backend.service.WritingAssistantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class WritingAssistantControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WritingAssistantService writingService;

    @InjectMocks
    private WritingAssistantController controller;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("learner@parrotalk.fun")
                .fullName("Test Learner")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/writing/analyze returns 200 and WritingSessionResponseDto")
    void testAnalyze_Success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        WritingSessionResponseDto responseDto = WritingSessionResponseDto.builder()
                .id(sessionId)
                .inputText("Người say rượu không được lái xe vì nồng độ cồn trong máu làm giảm tầm nhìn, giảm sự tập trung.")
                .inputLanguage("VI")
                .mode(WritingMode.SUGGESTION)
                .scaleType(DifficultyScale.IELTS)
                .targetLevel("6.5")
                .coreIdeaSummaryVn("Không được lái xe khi say rượu.")
                .vocabulary(List.of(
                        WritingSessionResponseDto.WritingLexicalItemDto.builder()
                                .id(UUID.randomUUID())
                                .text("impair")
                                .meaningVn("làm suy giảm")
                                .inDictionary(true)
                                .dictionaryWord("impair")
                                .validationStatus(WritingItemValidationStatus.APPROVED)
                                .build()
                ))
                .build();

        when(writingService.analyzeAndSave(any(WritingAnalyzeRequestDto.class), eq(testUser)))
                .thenReturn(responseDto);

        String requestBody = """
                {
                  "inputText": "Người say rượu không được lái xe vì nồng độ cồn trong máu làm giảm tầm nhìn, giảm sự tập trung.",
                  "mode": "SUGGESTION",
                  "scaleType": "IELTS",
                  "targetLevel": "6.5"
                }
                """;

        mockMvc.perform(post("/api/writing/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.result.mode").value("SUGGESTION"))
                .andExpect(jsonPath("$.result.scaleType").value("IELTS"))
                .andExpect(jsonPath("$.result.targetLevel").value("6.5"))
                .andExpect(jsonPath("$.result.vocabulary", hasSize(1)))
                .andExpect(jsonPath("$.result.vocabulary[0].text").value("impair"))
                .andExpect(jsonPath("$.result.vocabulary[0].inDictionary").value(true));

        verify(writingService, times(1)).analyzeAndSave(any(WritingAnalyzeRequestDto.class), eq(testUser));
    }

    @Test
    @DisplayName("POST /api/writing/analyze with blank input returns 400 Bad Request")
    void testAnalyze_BlankInput_ReturnsBadRequest() throws Exception {
        String invalidBody = """
                {
                  "inputText": "  ",
                  "mode": "SUGGESTION",
                  "scaleType": "CEFR",
                  "targetLevel": "B1"
                }
                """;

        mockMvc.perform(post("/api/writing/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(writingService, never()).analyzeAndSave(any(), any());
    }

    @Test
    @DisplayName("GET /api/writing/sessions returns paginated session history")
    void testGetSessions_Success() throws Exception {
        WritingSessionSummaryDto summary = WritingSessionSummaryDto.builder()
                .id(UUID.randomUUID())
                .inputText("Người say rượu không được lái xe.")
                .mode(WritingMode.GUIDANCE)
                .scaleType(DifficultyScale.CEFR)
                .targetLevel("B2")
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<WritingSessionSummaryDto> pageResponse = PageResponse.<WritingSessionSummaryDto>builder()
                .content(List.of(summary))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(writingService.getUserSessions(eq(testUser), eq(0), eq(10)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/writing/sessions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content", hasSize(1)))
                .andExpect(jsonPath("$.result.content[0].targetLevel").value("B2"));
    }

    @Test
    @DisplayName("PATCH /api/writing/sessions/{id}/draft updates draft and returns grammar validation")
    void testUpdateDraft_Success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        WritingSessionResponseDto updatedDto = WritingSessionResponseDto.builder()
                .id(sessionId)
                .userDraft("Drunk people must not drive because alcohol impairs their vision.")
                .draftValidation(GrammarValidationResultDto.builder()
                        .valid(true)
                        .status(WritingItemValidationStatus.APPROVED)
                        .summaryVn("Cấu trúc câu đạt chuẩn.")
                        .build())
                .build();

        when(writingService.updateUserDraft(eq(sessionId),
                eq("Drunk people must not drive because alcohol impairs their vision."), eq(testUser)))
                .thenReturn(updatedDto);

        mockMvc.perform(patch("/api/writing/sessions/" + sessionId + "/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userDraft\": \"Drunk people must not drive because alcohol impairs their vision.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.userDraft").value("Drunk people must not drive because alcohol impairs their vision."))
                .andExpect(jsonPath("$.result.draftValidation.valid").value(true));
    }
}


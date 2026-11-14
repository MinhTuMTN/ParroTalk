package com.parrotalk.backend.controller;

import com.parrotalk.backend.constant.DifficultyScale;
import com.parrotalk.backend.constant.WritingItemValidationStatus;
import com.parrotalk.backend.constant.WritingMode;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.writing.GrammarValidationResultDto;
import com.parrotalk.backend.dto.writing.WritingAnalyzeRequestDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionSummaryDto;
import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.GlobalExceptionHandler;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.service.TtsPrewarmService;
import com.parrotalk.backend.service.WritingAssistantService;
import com.parrotalk.backend.service.WritingTtsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    @Mock
    private WritingTtsService writingTtsService;

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

    @Test
    @DisplayName("GET /api/writing/tts returns 200 OK with audio/mpeg and immutable cache headers")
    void testSynthesizeTts_Success() throws Exception {
        byte[] audioBytes = new byte[] { 0x49, 0x44, 0x33, 0x03 };
        when(writingTtsService.synthesize("impair", "UK")).thenReturn(audioBytes);

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "impair")
                        .param("accent", "UK"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=604800, immutable"))
                .andExpect(content().bytes(audioBytes));

        verify(writingTtsService, times(1)).synthesize("impair", "UK");
    }

    @Test
    @DisplayName("GET /api/writing/tts defaults accent to US when omitted")
    void testSynthesizeTts_DefaultAccent() throws Exception {
        byte[] audioBytes = new byte[] { 0x49, 0x44, 0x33 };
        when(writingTtsService.synthesize("hello world", "US")).thenReturn(audioBytes);

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "hello world"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(audioBytes));

        verify(writingTtsService, times(1)).synthesize("hello world", "US");
    }

    @Test
    @DisplayName("GET /api/writing/tts returns 400 Bad Request when text is invalid")
    void testSynthesizeTts_InvalidText_ReturnsBadRequest() throws Exception {
        when(writingTtsService.synthesize("   ", "US"))
                .thenThrow(new ParroTalkException(
                        "TTS text must not be blank.",
                        ErrorCode.VALIDATION_ERROR.name(),
                        HttpStatus.BAD_REQUEST
                ));

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/writing/tts delegates custom voice parameter when non-blank and uses 2-arg overload when blank")
    void testSynthesizeTts_WithOptionalVoice() throws Exception {
        byte[] guyBytes = new byte[] { 0x49, 0x44, 0x33, 0x05 };
        byte[] defaultBytes = new byte[] { 0x49, 0x44, 0x33, 0x06 };
        when(writingTtsService.synthesize("impair", "US", "en-US-GuyNeural")).thenReturn(guyBytes);
        when(writingTtsService.synthesize("impair", "US")).thenReturn(defaultBytes);

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "impair")
                        .param("accent", "US")
                        .param("voice", "en-US-GuyNeural"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(guyBytes));

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "impair")
                        .param("accent", "US")
                        .param("voice", "   "))
                .andExpect(status().isOk())
                .andExpect(content().bytes(defaultBytes));

        verify(writingTtsService, times(1)).synthesize("impair", "US", "en-US-GuyNeural");
        verify(writingTtsService, times(1)).synthesize("impair", "US");
    }

    @Test
    @DisplayName("GET /api/writing/tts returns 400 Bad Request when text query param is missing")
    void testSynthesizeTts_MissingTextParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/writing/tts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(writingTtsService, never()).synthesize(any(), any());
    }

    @Test
    @DisplayName("GET /api/writing/tts serves cached FileSystemResource with ETag, 304 Not Modified, and 206 Partial Content")
    void testSynthesizeTts_CachedFileETagAndByteRange() throws Exception {
        Path tempFile = Files.createTempFile("writing-tts-test-", ".mp3");
        try {
            byte[] content = "0123456789".getBytes(StandardCharsets.US_ASCII);
            Files.write(tempFile, content);
            when(writingTtsService.resolveCachedFileIfExists("cached hello", "US", null))
                    .thenReturn(Optional.of(tempFile));

            String expectedETag = mockMvc.perform(get("/api/writing/tts")
                            .param("text", "cached hello")
                            .param("accent", "US"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("audio/mpeg"))
                    .andExpect(header().exists(HttpHeaders.ETAG))
                    .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=604800, immutable"))
                    .andExpect(content().bytes(content))
                    .andReturn()
                    .getResponse()
                    .getHeader(HttpHeaders.ETAG);

            mockMvc.perform(get("/api/writing/tts")
                            .param("text", "cached hello")
                            .param("accent", "US")
                            .header(HttpHeaders.IF_NONE_MATCH, expectedETag))
                    .andExpect(status().isNotModified())
                    .andExpect(header().string(HttpHeaders.ETAG, expectedETag));

            mockMvc.perform(get("/api/writing/tts")
                            .param("text", "cached hello")
                            .param("accent", "US")
                            .header(HttpHeaders.RANGE, "bytes=2-5"))
                    .andExpect(status().isPartialContent())
                    .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 2-5/10"))
                    .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                    .andExpect(content().bytes("2345".getBytes(StandardCharsets.US_ASCII)));

            mockMvc.perform(get("/api/writing/tts")
                            .param("text", "cached hello")
                            .param("accent", "US")
                            .header(HttpHeaders.RANGE, "bytes=999-"))
                    .andExpect(status().isRequestedRangeNotSatisfiable())
                    .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */10"));

            verify(writingTtsService, never()).synthesize(any(), any());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("GET /api/writing/tts returns 302 Found to Cloudinary URL when cached in Postgres")
    void testSynthesizeTts_CloudinaryHit_Returns302Redirect() throws Exception {
        String cdnUrl = "https://res.cloudinary.com/demo/video/upload/tts/us_abc123.mp3";
        when(writingTtsService.resolveCloudinaryUrlIfCached("impair", "US", null))
                .thenReturn(Optional.of(cdnUrl));

        mockMvc.perform(get("/api/writing/tts")
                        .param("text", "impair")
                        .param("accent", "US"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, cdnUrl))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=604800, immutable"));

        verify(writingTtsService, never()).synthesize(any(), any());
        verify(writingTtsService, never()).synthesize(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/writing/tts enqueues RabbitMQ job and returns 503 when localFallbackEnabled is false on cache miss")
    void testSynthesizeTts_LocalFallbackDisabled_EnqueuesAndReturns503() throws Exception {
        TtsPrewarmService prewarmMock = mock(TtsPrewarmService.class);
        controller.setTtsPrewarmService(prewarmMock);
        controller.setLocalFallbackEnabled(false);
        try {
            when(writingTtsService.resolveCloudinaryUrlIfCached("impair", "UK", "en-GB-RyanNeural"))
                    .thenReturn(Optional.empty());
            when(writingTtsService.resolveCachedFileIfExists("impair", "UK", "en-GB-RyanNeural"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/writing/tts")
                            .param("text", "impair")
                            .param("accent", "UK")
                            .param("voice", "en-GB-RyanNeural"))
                    .andExpect(status().isServiceUnavailable());

            verify(prewarmMock, times(1)).enqueueSingleSynthesis("impair", "UK", "en-GB-RyanNeural");
            verify(writingTtsService, never()).synthesize(any(), any());
            verify(writingTtsService, never()).synthesize(any(), any(), any());
        } finally {
            controller.setLocalFallbackEnabled(true);
            controller.setTtsPrewarmService(null);
        }
    }
}


package com.parrotalk.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.constant.Difficulty;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.LessonVisibilityStatus;
import com.parrotalk.backend.constant.MediaType;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.SourceType;
import com.parrotalk.backend.dto.AnswerRequest;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.SectionLessonProjection;
import com.parrotalk.backend.dto.UpdateLessonSegmentsRequest;
import com.parrotalk.backend.dto.UpdateUserSettingsRequest;
import com.parrotalk.backend.dto.UpsertSegmentRequest;
import com.parrotalk.backend.dto.dictation.BlankAnswerItem;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;
import com.parrotalk.backend.dto.dictation.SegmentBlanksPayload;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserLessonHistory;
import com.parrotalk.backend.entity.UserLessonProgress;
import com.parrotalk.backend.repository.LessonRepository;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import com.parrotalk.backend.repository.UserLessonDraftSegmentRepository;
import com.parrotalk.backend.repository.UserLessonHistoryRepository;
import com.parrotalk.backend.repository.UserLessonProgressRepository;
import com.parrotalk.backend.repository.UserRepository;
import com.parrotalk.backend.security.JwtUtils;
import com.parrotalk.backend.service.AudioProcessingService;
import com.parrotalk.backend.service.dictation.DictationTokenizationService;
import com.parrotalk.backend.service.dictation.DictationTokenizationService.DictationToken;
import com.parrotalk.backend.util.SegmentBlanksCodec;

import jakarta.persistence.EntityManager;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DictationFillBlankIntegrationTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private TranscriptionSegmentRepository segmentRepository;

    @Autowired
    private UserLessonProgressRepository progressRepository;

    @Autowired
    private UserLessonDraftSegmentRepository draftSegmentRepository;

    @Autowired
    private UserLessonHistoryRepository historyRepository;

    @Autowired
    private AudioProcessingService audioProcessingService;

    @Autowired
    private DictationTokenizationService tokenizationService;

    @Autowired
    private SegmentBlanksCodec segmentBlanksCodec;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private User learner;
    private User admin;
    private String learnerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        if (cacheManager != null) {
            for (String name : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        learner = userRepository.saveAndFlush(User.builder()
                .email("learner-" + suffix + "@parrotalk.fun")
                .displayUsername("learner_" + suffix)
                .fullName("Learner User")
                .password("$2a$10$hash")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());
        learnerToken = jwtUtils.generateToken(learner);

        admin = userRepository.saveAndFlush(User.builder()
                .email("admin-" + suffix + "@parrotalk.fun")
                .displayUsername("admin_" + suffix)
                .fullName("Admin User")
                .password("$2a$10$hash")
                .role(Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build());
        adminToken = jwtUtils.generateToken(admin);
    }

    @Test
    @DisplayName("Audio ingestion generates and stores blanks_json automatically for new segments")
    void audioIngestionGeneratesAndPersistsBlanksJson() {
        Lesson lesson = lessonRepository.saveAndFlush(Lesson.builder()
                .title("Ingested Audio Lesson")
                .fileUrl("https://cdn.example.com/ingest.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.PROCESSING)
                .visibilityStatus(LessonVisibilityStatus.HIDDEN)
                .build());

        JsonNode payload = JsonMapper.builder().build().valueToTree(Map.of(
                "lessonId", lesson.getId().toString(),
                "status", "COMPLETED",
                "duration", 45,
                "result", Map.of(
                        "segments", List.of(
                                Map.of("text", "My colleague carefully reviewed the quarterly report yesterday.", "start", 0.0, "end", 4.5),
                                Map.of("text", "[Music]", "start", 4.5, "end", 6.0)))));

        audioProcessingService.process(payload);

        List<TranscriptionSegment> savedSegments = segmentRepository.findByLessonIdOrderByDisplayOrderAsc(lesson.getId());
        assertThat(savedSegments).hasSize(2);
        assertThat(savedSegments.get(0).getBlanksJson()).isNotBlank();

        SegmentBlanksPayload parsedFirst = segmentBlanksCodec.deserialize(savedSegments.get(0).getBlanksJson());
        assertThat(parsedFirst.isSuitable()).isTrue();
        assertThat(parsedFirst.getWordCount()).isEqualTo(8);
        assertThat(parsedFirst.getLevels().get(DictationMode.EASY)).isNotEmpty();
        assertThat(parsedFirst.getLevels().get(DictationMode.MEDIUM)).isNotEmpty();
        assertThat(parsedFirst.getLevels().get(DictationMode.HARD)).isNotEmpty();

        SegmentBlanksPayload parsedSecond = segmentBlanksCodec.deserialize(savedSegments.get(1).getBlanksJson());
        assertThat(parsedSecond.isSuitable()).isFalse();
        assertThat(parsedSecond.getLevels().get(DictationMode.EASY)).isEmpty();
    }

    @Test
    @DisplayName("Legacy segment with null blanks_json triggers lazy backfill on lesson detail GET and admin segment update regenerates blanks")
    void lazyBackfillOnDetailAndRegenerateOnSegmentUpdate() throws Exception {
        Lesson lesson = lessonRepository.saveAndFlush(Lesson.builder()
                .title("Legacy Lesson")
                .fileUrl("https://cdn.example.com/legacy.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .totalSegments(1)
                .build());

        TranscriptionSegment legacySegment = segmentRepository.saveAndFlush(TranscriptionSegment.builder()
                .lesson(lesson)
                .text("People often drink warm coffee before morning meetings.")
                .startTime(0.0)
                .endTime(3.5)
                .displayOrder(1)
                .difficulty(Difficulty.MEDIUM)
                .blanksJson(null)
                .build());

        entityManager.clear();

        mockMvc.perform(get("/api/lessons/" + lesson.getId())
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].wordCount").value(8))
                .andExpect(jsonPath("$.segments[0].suitableForBlanks").value(true))
                .andExpect(jsonPath("$.segments[0].blanks.EASY").isArray())
                .andExpect(jsonPath("$.segments[0].blanks.MEDIUM").isArray())
                .andExpect(jsonPath("$.segments[0].blanks.HARD").isArray());

        TranscriptionSegment backfilled = segmentRepository.findById(legacySegment.getId()).orElseThrow();
        assertThat(backfilled.getBlanksJson()).isNotBlank();

        // Admin edits segment text -> blanksJson must be regenerated
        String oldBlanksJson = backfilled.getBlanksJson();
        UpdateLessonSegmentsRequest updateReq = new UpdateLessonSegmentsRequest();
        UpsertSegmentRequest segReq = new UpsertSegmentRequest();
        segReq.setId(legacySegment.getId());
        segReq.setText("Our engineers built fifteen fast servers during the weekend.");
        segReq.setStartTime(0.0);
        segReq.setEndTime(4.0);
        segReq.setOrder(1);
        updateReq.setSegments(List.of(segReq));

        mockMvc.perform(put("/api/admin/lessons/" + lesson.getId() + "/segments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].wordCount").value(9))
                .andExpect(jsonPath("$.segments[0].suitableForBlanks").value(true));

        TranscriptionSegment updatedSeg = segmentRepository.findById(legacySegment.getId()).orElseThrow();
        assertThat(updatedSeg.getBlanksJson()).isNotEqualTo(oldBlanksJson);
    }

    @Test
    @DisplayName("Mode-scoped progress, answer validation, wrong-attempt score=0, replay/hint, submit-lesson, FULL compat, and non-duplicating library queries")
    void endToEndModeScopedFlowAndLibraryNonDuplication() throws Exception {
        Lesson lesson = lessonRepository.saveAndFlush(Lesson.builder()
                .title("Multi-Mode Practice Lesson")
                .fileUrl("https://cdn.example.com/practice.mp3")
                .mediaType(MediaType.AUDIO)
                .sourceType(SourceType.CLOUDINARY)
                .status(LessonStatus.DONE)
                .visibilityStatus(LessonVisibilityStatus.PUBLISHED)
                .totalSegments(2)
                .build());

        TranscriptionSegment seg1 = segmentRepository.saveAndFlush(TranscriptionSegment.builder()
                .lesson(lesson)
                .text("My friend didn't bring fifteen notebooks yesterday.")
                .startTime(0.0)
                .endTime(3.0)
                .displayOrder(1)
                .difficulty(Difficulty.MEDIUM)
                .build());

        TranscriptionSegment seg2 = segmentRepository.saveAndFlush(TranscriptionSegment.builder()
                .lesson(lesson)
                .text("She carefully wrote the weekly summary report.")
                .startTime(3.0)
                .endTime(6.0)
                .displayOrder(2)
                .difficulty(Difficulty.MEDIUM)
                .build());

        entityManager.clear();

        // 1. Trigger lazy blanks generation via GET /api/lessons/{id}
        mockMvc.perform(get("/api/lessons/" + lesson.getId())
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments.length()").value(2));

        TranscriptionSegment loadedSeg1 = segmentRepository.findById(seg1.getId()).orElseThrow();
        SegmentBlanksPayload payload1 = segmentBlanksCodec.deserialize(loadedSeg1.getBlanksJson());
        List<BlankSlotDto> mediumBlanks1 = payload1.getLevels().get(DictationMode.MEDIUM);
        assertThat(mediumBlanks1).isNotEmpty();

        // 2. Wrong attempt in MEDIUM mode -> correct=false, score=0, concealed expectedAnswer on wrong blank
        List<BlankAnswerItem> wrongAnswers = mediumBlanks1.stream()
                .map(slot -> BlankAnswerItem.builder()
                        .wordIndex(slot.getWordIndex())
                        .answer("wrongword")
                        .build())
                .toList();

        AnswerRequest wrongMediumReq = AnswerRequest.builder()
                .segmentId(seg1.getId())
                .mode(DictationMode.MEDIUM)
                .blankAnswers(wrongAnswers)
                .build();

        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/answer")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongMediumReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.accuracy").value(0.0))
                .andExpect(jsonPath("$.blankResults[0].correct").value(false))
                .andExpect(jsonPath("$.blankResults[0].expectedAnswer").doesNotExist());

        // 3. Mode-scoped replay & hint increment in MEDIUM mode should NOT affect FULL mode
        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/segments/" + seg1.getId() + "/replay")
                        .param("mode", "MEDIUM")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/segments/" + seg1.getId() + "/hint")
                        .param("mode", "MEDIUM")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lessons/" + lesson.getId() + "/progress")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("FULL"))
                .andExpect(jsonPath("$.draftSegments.length()").value(0));

        // 4. Correct submission in MEDIUM mode (with case/whitespace variation)
        List<DictationToken> words1 = tokenizationService.extractWordTokens(seg1.getText());
        List<BlankAnswerItem> correctMediumAnswers = mediumBlanks1.stream()
                .map(slot -> BlankAnswerItem.builder()
                        .wordIndex(slot.getWordIndex())
                        .answer(" " + words1.get(slot.getWordIndex()).getText().toUpperCase() + " ")
                        .build())
                .toList();

        AnswerRequest correctMediumReq = AnswerRequest.builder()
                .segmentId(seg1.getId())
                .mode(DictationMode.MEDIUM)
                .blankAnswers(correctMediumAnswers)
                .build();

        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/answer")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctMediumReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.score").value(greaterThan(0)))
                .andExpect(jsonPath("$.accuracy").value(1.0))
                .andExpect(jsonPath("$.replayCount").value(1))
                .andExpect(jsonPath("$.hintCount").value(1));

        // 5. FULL mode backward-compatible request (omitted mode defaults to FULL)
        AnswerRequest legacyFullReq = AnswerRequest.builder()
                .segmentId(seg1.getId())
                .userAnswer(seg1.getText())
                .build();

        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/answer")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legacyFullReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.score").value(10));

        // 6. Both MEDIUM (progress=0.5) and FULL (progress=0.5) rows exist -> library queries MUST NOT duplicate lesson
        List<UserLessonProgress> allProgress = progressRepository.findByUserIdAndLessonIdIn(learner.getId(), List.of(lesson.getId()));
        assertThat(allProgress).hasSize(2);

        Page<LessonWithProgressDTO> publicSearch = lessonRepository.searchPublicLessons(
                "Multi-Mode", null, null, null, learner.getId(), PageRequest.of(0, 10));
        assertThat(publicSearch.getTotalElements()).isEqualTo(1);
        assertThat(publicSearch.getContent()).hasSize(1);

        List<LessonWithProgressDTO> continueLearning = lessonRepository.findContinueLearning(learner.getId(), PageRequest.of(0, 10));
        assertThat(continueLearning).hasSize(1);

        List<SectionLessonProjection> batchUserLessons = lessonRepository.findBatchUserLessons(learner.getId(), 10);
        assertThat(batchUserLessons).hasSize(1);

        // 7. Submitting an incomplete lesson (only 1 of 2 segments completed) must fail with HTTP 400
        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/submit-lesson")
                        .param("mode", "MEDIUM")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isBadRequest());

        // 8. Answering segment 2 incorrectly still leaves the lesson incomplete -> HTTP 400 on submit-lesson
        TranscriptionSegment loadedSeg2 = segmentRepository.findById(seg2.getId()).orElseThrow();
        SegmentBlanksPayload payload2 = segmentBlanksCodec.deserialize(loadedSeg2.getBlanksJson());
        List<BlankSlotDto> mediumBlanks2 = payload2.getLevels().get(DictationMode.MEDIUM);
        assertThat(mediumBlanks2).isNotEmpty();

        List<BlankAnswerItem> wrongMediumAnswers2 = mediumBlanks2.stream()
                .map(slot -> BlankAnswerItem.builder()
                        .wordIndex(slot.getWordIndex())
                        .answer("wrongword")
                        .build())
                .toList();

        AnswerRequest wrongMediumReq2 = AnswerRequest.builder()
                .segmentId(seg2.getId())
                .mode(DictationMode.MEDIUM)
                .blankAnswers(wrongMediumAnswers2)
                .build();

        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/answer")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongMediumReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));

        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/submit-lesson")
                        .param("mode", "MEDIUM")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isBadRequest());

        assertThat(historyRepository.findByUserId(learner.getId())).isEmpty();

        // 9. Complete segment 2 correctly in MEDIUM mode, then submit lesson -> HTTP 200 and cleans up only MEDIUM drafts
        List<DictationToken> words2 = tokenizationService.extractWordTokens(seg2.getText());
        List<BlankAnswerItem> correctMediumAnswers2 = mediumBlanks2.stream()
                .map(slot -> BlankAnswerItem.builder()
                        .wordIndex(slot.getWordIndex())
                        .answer(words2.get(slot.getWordIndex()).getText())
                        .build())
                .toList();

        AnswerRequest correctMediumReq2 = AnswerRequest.builder()
                .segmentId(seg2.getId())
                .mode(DictationMode.MEDIUM)
                .blankAnswers(correctMediumAnswers2)
                .build();

        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/answer")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctMediumReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(post("/api/lessons/" + lesson.getId() + "/submit-lesson")
                        .param("mode", "MEDIUM")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));

        assertThat(draftSegmentRepository.findByUserIdAndLessonIdAndMode(learner.getId(), lesson.getId(), DictationMode.MEDIUM)).isEmpty();
        assertThat(draftSegmentRepository.findByUserIdAndLessonIdAndMode(learner.getId(), lesson.getId(), DictationMode.FULL)).hasSize(1);

        List<UserLessonHistory> histories = historyRepository.findByUserId(learner.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getDictationMode()).isEqualTo(DictationMode.MEDIUM);

        // 10. Reset FULL mode progress
        mockMvc.perform(get("/api/lessons/" + lesson.getId() + "/progress/reset")
                        .param("mode", "FULL")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk());
        assertThat(draftSegmentRepository.findByUserIdAndLessonIdAndMode(learner.getId(), lesson.getId(), DictationMode.FULL)).isEmpty();
    }

    @Test
    @DisplayName("UserSettings GET/PUT persists and validates defaultDictationMode")
    void userSettingsDefaultDictationModeEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/me/settings")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDictationMode").value("FULL"));

        UpdateUserSettingsRequest req = UpdateUserSettingsRequest.builder()
                .defaultDictationMode("hard")
                .build();

        mockMvc.perform(put("/api/users/me/settings")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDictationMode").value("HARD"));

        mockMvc.perform(get("/api/users/me/settings")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDictationMode").value("HARD"));
    }
}

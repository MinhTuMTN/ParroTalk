package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.practice.FillBlankQuestionDto;
import com.parrotalk.backend.dto.practice.LearnWordDto;
import com.parrotalk.backend.dto.practice.MultipleChoiceQuestionDto;
import com.parrotalk.backend.dto.practice.TypeWordQuestionDto;
import com.parrotalk.backend.service.VocabularyPracticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class VocabularyPracticeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VocabularyPracticeService practiceService;

    @InjectMocks
    private VocabularyPracticeController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Endpoint 1: GET /api/vocabulary/practice/fill-blank returns 200 and ApiResponse with code 1000")
    void testGetFillBlankQuestions_Success() throws Exception {
        FillBlankQuestionDto dto = FillBlankQuestionDto.builder()
                .id(UUID.randomUUID())
                .word("sustainable")
                .targetForm("sustainable")
                .sentenceWithBlank("Solar energy is a ______ resource.")
                .completedSentence("Solar energy is a sustainable resource.")
                .sentenceVn("Năng lượng mặt trời là một nguồn tài nguyên bền vững.")
                .grammarContextTip("Adjective modifying noun 'resource'.")
                .explanation("'sustainable' means able to be maintained.")
                .source("DATABASE")
                .build();

        when(practiceService.getFillBlankQuestions("Environment", 10))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/vocabulary/practice/fill-blank")
                        .param("topic", "Environment")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].word").value("sustainable"))
                .andExpect(jsonPath("$.result[0].source").value("DATABASE"))
                .andExpect(jsonPath("$.result[0].sentenceWithBlank").value("Solar energy is a ______ resource."));

        verify(practiceService, times(1)).getFillBlankQuestions("Environment", 10);
    }

    @Test
    @DisplayName("Endpoint 1: GET /api/vocabulary/practice/fill-blank supports 'count' parameter alias")
    void testGetFillBlankQuestions_CountParamAlias() throws Exception {
        when(practiceService.getFillBlankQuestions("Environment", 5))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/vocabulary/practice/fill-blank")
                        .param("topic", "Environment")
                        .param("count", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result", empty()));

        verify(practiceService, times(1)).getFillBlankQuestions("Environment", 5);
    }

    @Test
    @DisplayName("Endpoint 2: GET /api/vocabulary/practice/learn-words returns 200 and LearnWordDto list")
    void testGetLearnWords_Success() throws Exception {
        LearnWordDto card = LearnWordDto.builder()
                .id(UUID.randomUUID())
                .word("innovate")
                .phonetic("/ˈɪn.ə.veɪt/")
                .phoneticUs("/ˈɪn.ə.veɪt/")
                .phoneticUk("/ˈɪn.ə.veɪt/")
                .audioUrlUs("https://audio.parrotalk.fun/us/innovate.mp3")
                .partOfSpeech("verb")
                .cefrLevel("B2")
                .topic("Technology")
                .definitionEn("to introduce something new")
                .definitionVn("đổi mới")
                .build();

        when(practiceService.getLearnWordCards("Technology", 15))
                .thenReturn(List.of(card));

        mockMvc.perform(get("/api/vocabulary/practice/learn-words")
                        .param("topic", "Technology")
                        .param("limit", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].word").value("innovate"))
                .andExpect(jsonPath("$.result[0].audioUrlUs").value("https://audio.parrotalk.fun/us/innovate.mp3"))
                .andExpect(jsonPath("$.result[0].partOfSpeech").value("verb"));

        verify(practiceService, times(1)).getLearnWordCards("Technology", 15);
    }

    @Test
    @DisplayName("Endpoint 3: GET /api/vocabulary/practice/multiple-choice returns 200 with options and correctIndex")
    void testGetMultipleChoice_Success() throws Exception {
        MultipleChoiceQuestionDto mcq = MultipleChoiceQuestionDto.builder()
                .id(UUID.randomUUID())
                .question("What is the meaning of 'innovate'?")
                .targetWord("innovate")
                .options(List.of("to introduce something new", "to destroy", "to remain static", "to follow rules"))
                .correctAnswer("to introduce something new")
                .correctIndex(0)
                .build();

        when(practiceService.getMultipleChoiceQuestions("Technology", 4))
                .thenReturn(List.of(mcq));

        mockMvc.perform(get("/api/vocabulary/practice/multiple-choice")
                        .param("topic", "Technology")
                        .param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].targetWord").value("innovate"))
                .andExpect(jsonPath("$.result[0].correctIndex").value(0))
                .andExpect(jsonPath("$.result[0].options", hasSize(4)));

        verify(practiceService, times(1)).getMultipleChoiceQuestions("Technology", 4);
    }

    @Test
    @DisplayName("Endpoint 4: GET /api/vocabulary/practice/type-word returns 200 with masked example and firstLetter")
    void testGetTypeWord_Success() throws Exception {
        TypeWordQuestionDto tw = TypeWordQuestionDto.builder()
                .id(UUID.randomUUID())
                .targetWord("innovate")
                .promptDefinition("to introduce something new (đổi mới)")
                .firstLetter("i")
                .characterCount(8)
                .exampleWithMask("Engineers ________ daily.")
                .build();

        when(practiceService.getTypeWordQuestions("Technology", 10))
                .thenReturn(List.of(tw));

        mockMvc.perform(get("/api/vocabulary/practice/type-word")
                        .param("topic", "Technology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].targetWord").value("innovate"))
                .andExpect(jsonPath("$.result[0].firstLetter").value("i"))
                .andExpect(jsonPath("$.result[0].characterCount").value(8))
                .andExpect(jsonPath("$.result[0].exampleWithMask").value("Engineers ________ daily."));

        verify(practiceService, times(1)).getTypeWordQuestions("Technology", 10);
    }

    @Test
    @DisplayName("Edge Case: Missing topic parameter returns 400 Bad Request")
    void testMissingTopicParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/vocabulary/practice/fill-blank"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/vocabulary/practice/learn-words"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/vocabulary/practice/multiple-choice"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/vocabulary/practice/type-word"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case: Special characters and Unicode in topic query param")
    void testSpecialCharactersInTopic() throws Exception {
        when(practiceService.getFillBlankQuestions(eq("Arts & Culture"), anyInt()))
                .thenReturn(List.of());
        when(practiceService.getFillBlankQuestions(eq("Doctor's Office"), anyInt()))
                .thenReturn(List.of());
        when(practiceService.getFillBlankQuestions(eq("Môi trường"), anyInt()))
                .thenReturn(List.of());

        // Ampersand
        mockMvc.perform(get("/api/vocabulary/practice/fill-blank")
                        .param("topic", "Arts & Culture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // Single quote
        mockMvc.perform(get("/api/vocabulary/practice/fill-blank")
                        .param("topic", "Doctor's Office"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // Vietnamese UTF-8 Unicode
        mockMvc.perform(get("/api/vocabulary/practice/fill-blank")
                        .param("topic", "Môi trường"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    @DisplayName("Backward Compatibility: /questions and /submit endpoints")
    void testBackwardCompatibilityEndpoints() throws Exception {
        when(practiceService.generateQuestions("Business", "B2", 5, "fill-blank"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/vocabulary/practice/questions")
                        .param("topic", "Business")
                        .param("cefrLevel", "B2")
                        .param("count", "5")
                        .param("mode", "fill-blank"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vocabulary/practice/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());

        verify(practiceService, times(1)).submitPractice(any());
    }
}

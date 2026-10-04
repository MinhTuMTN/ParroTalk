package com.parrotalk.backend.service.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiRefineRequestDto;
import com.parrotalk.backend.dto.admin.vocab.LlmSenseRefineResponseItem;
import com.parrotalk.backend.service.AiCoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminVocabularyAiService {

    private final AiCoreClient aiCoreClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are an expert linguist and English dictionary editor.
            Your task is to refine vocabulary senses to ensure high-quality dictionary entries.
            For each sense provided in the request:
            1. Suggest or refine the CEFR level (A1, A2, B1, B2, C1, C2). If it is missing, Unknown or invalid, estimate it based on the word and definition.
            2. Refine the English definition to be clear, concise, and accurate for the given part of speech.
            3. Refine the Vietnamese definition to naturally reflect the English meaning.
            4. Create or refine a natural, commonly used English example sentence demonstrating the word. IMPORTANT: The example sentence MUST explicitly contain the vocabulary word being defined.
            5. Provide a natural Vietnamese translation for the example sentence.

            Output ONLY a JSON array, no markdown/comments and do not include any reasoning or code blocks.
            Each item MUST strictly follow this structure: 
            {"index":<int>,"cefrLevel":"<string>","definitionEn":"<string>","definitionVn":"<string>","exampleEn":"<string>","exampleVn":"<string>"}
            """;

    private static final ParameterizedTypeReference<List<LlmSenseRefineResponseItem>> LLM_RESPONSE_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    public List<LlmSenseRefineResponseItem> refineSenses(AdminVocabularyAiRefineRequestDto request) {
        if (request.getSenses() == null || request.getSenses().isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> indexedSenses = new ArrayList<>();
        for (int i = 0; i < request.getSenses().size(); i++) {
            Map<String, Object> senseMap = new HashMap<>();
            senseMap.put("index", i);
            senseMap.put("sense", request.getSenses().get(i));
            indexedSenses.add(senseMap);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("word", request.getWord());
        payload.put("sensesToRefine", indexedSenses);

        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize AI refine request payload to JSON", e);
        }

        return aiCoreClient.callAndValidate(
                SYSTEM_PROMPT,
                userPrompt,
                LLM_RESPONSE_LIST_TYPE,
                rawResponse -> {
                    if (rawResponse == null) {
                        throw new IllegalArgumentException("AI returned null response");
                    }
                    if (rawResponse.size() != request.getSenses().size()) {
                        throw new IllegalArgumentException("AI returned " + rawResponse.size() + " items, expected " + request.getSenses().size());
                    }
                    return rawResponse;
                }
        );
    }

    private static final String COMMON_MEANING_SYSTEM_PROMPT = """
            You are an expert linguist and English-Vietnamese translator.
            Your task is to provide the most common Vietnamese meaning(s) for the given English word.
            Provide a short, concise, natural-sounding Vietnamese translation (usually a few words, separated by commas).
            
            Output ONLY a JSON object, no markdown/comments and do not include any reasoning or code blocks.
            The JSON object MUST strictly follow this structure:
            {"commonMeaningVn":"<string>"}
            """;

    private static final ParameterizedTypeReference<com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiCommonMeaningResponseDto> COMMON_MEANING_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    public com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiCommonMeaningResponseDto generateCommonMeaningVn(
            com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiCommonMeaningRequestDto request) {
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("word", request.getWord());

        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize AI request payload to JSON", e);
        }

        return aiCoreClient.callAndValidate(
                COMMON_MEANING_SYSTEM_PROMPT,
                userPrompt,
                COMMON_MEANING_RESPONSE_TYPE,
                rawResponse -> {
                    if (rawResponse == null || rawResponse.getCommonMeaningVn() == null || rawResponse.getCommonMeaningVn().isBlank()) {
                        throw new IllegalArgumentException("AI returned invalid common meaning");
                    }
                    return rawResponse;
                }
        );
    }
}

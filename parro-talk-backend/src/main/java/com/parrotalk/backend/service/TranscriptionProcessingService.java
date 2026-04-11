package com.parrotalk.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.entity.LessonStatus;
import com.parrotalk.backend.entity.SegmentType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionProcessingService {
    private final LessonService lessonService;
    private final TranscriptionSegmentRepository segmentRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.python-service.url}")
    private String pythonUploadUrl;

    public void startTranscription(UUID lessonId, MultipartFile file) {
        try {
            lessonService.updateProgress(lessonId, 5, "Uploading to AI Service", LessonStatus.PROCESSING);

            RestTemplate restTemplate = new RestTemplate();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(pythonUploadUrl, requestEntity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !responseBody.containsKey("websocket_url")) {
                lessonService.updateProgress(lessonId, 0, "Failed to initialize remote task", LessonStatus.FAILED);
                return;
            }

            String websocketUrl = responseBody.get("websocket_url").toString();
            connectToWebSocket(websocketUrl, lessonId);

        } catch (Exception e) {
            lessonService.updateProgress(lessonId, 0, "Failed: " + e.getMessage(), LessonStatus.FAILED);
            e.printStackTrace();
        }
    }

    private void connectToWebSocket(String url, UUID lessonId) {
        StandardWebSocketClient client = new StandardWebSocketClient();
        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    lessonService.updateProgress(lessonId, 10, "Connected to AI Engine", LessonStatus.PROCESSING);
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    JsonNode node = mapper.readTree(message.getPayload());
                    String status = node.get("status").asText();

                    if ("starting".equals(status) || "extracting".equals(status) || "transcribing".equals(status)) {
                        String stepMsg = node.get("message").asText();
                        lessonService.updateProgress(lessonId, 10, stepMsg, LessonStatus.PROCESSING);
                    } else if ("processing".equals(status)) {
                        int progress = node.get("progress").asInt();
                        lessonService.updateProgress(lessonId, progress, "Processing", LessonStatus.PROCESSING);
                    } else if ("completed".equals(status)) {
                        JsonNode result = node.get("result");
                        saveRealResults(lessonId, result);
                        lessonService.updateProgress(lessonId, 100, "Completed", LessonStatus.DONE);
                        session.close();
                    } else if ("failed".equals(status)) {
                        String error = node.get("error").asText();
                        lessonService.updateProgress(lessonId, 0, "AI Error: " + error, LessonStatus.FAILED);
                        session.close();
                    }
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    lessonService.updateProgress(lessonId, 0, "WebSocket Error: " + exception.getMessage(),
                            LessonStatus.FAILED);
                }
            }, url);
        } catch (Exception e) {
            lessonService.updateProgress(lessonId, 0, "WebSocket connection failed: " + e.getMessage(),
                    LessonStatus.FAILED);
        }
    }

    private void saveRealResults(UUID lessonId, JsonNode resultNode) {
        if (!resultNode.has("segments"))
            return;

        for (JsonNode seg : resultNode.get("segments")) {
            TranscriptionSegment segment = TranscriptionSegment.builder()
                    .lessonId(lessonId)
                    .text(seg.get("text").asText().trim())
                    .startTime(seg.get("start").asDouble())
                    .endTime(seg.get("end").asDouble())
                    .type(SegmentType.SENTENCE)
                    .build();
            segmentRepository.save(segment);

            if (seg.has("words")) {
                for (JsonNode w : seg.get("words")) {
                    TranscriptionSegment word = TranscriptionSegment.builder()
                            .lessonId(lessonId)
                            .text(w.get("word").asText().trim())
                            .startTime(w.get("start").asDouble())
                            .endTime(w.get("end").asDouble())
                            .type(SegmentType.WORD)
                            .build();
                    segmentRepository.save(word);
                }
            }
        }
    }
}

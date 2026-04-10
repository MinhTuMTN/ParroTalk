package com.parrotalk.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.entity.JobStatus;
import com.parrotalk.backend.entity.SegmentType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
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
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Service
public class TranscriptionProcessingService {
    private final JobService jobService;
    private final TranscriptionSegmentRepository segmentRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.python-service.url}")
    private String pythonUploadUrl;

    public TranscriptionProcessingService(JobService jobService, TranscriptionSegmentRepository segmentRepository) {
        this.jobService = jobService;
        this.segmentRepository = segmentRepository;
    }

    public void startTranscription(UUID jobId, MultipartFile file) {
        try {
            jobService.updateProgress(jobId, 5, "Uploading to AI Service", JobStatus.PROCESSING);

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
                jobService.updateProgress(jobId, 0, "Failed to initialize remote task", JobStatus.FAILED);
                return;
            }

            String websocketUrl = responseBody.get("websocket_url").toString();
            connectToWebSocket(websocketUrl, jobId);

        } catch (Exception e) {
            jobService.updateProgress(jobId, 0, "Failed: " + e.getMessage(), JobStatus.FAILED);
            e.printStackTrace();
        }
    }

    private void connectToWebSocket(String url, UUID jobId) {
        StandardWebSocketClient client = new StandardWebSocketClient();
        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    jobService.updateProgress(jobId, 10, "Connected to AI Engine", JobStatus.PROCESSING);
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    JsonNode node = mapper.readTree(message.getPayload());
                    String status = node.get("status").asText();

                    if ("processing".equals(status)) {
                        int progress = node.has("progress") ? node.get("progress").asInt() : 0;
                        String stepMsg = node.has("message") ? node.get("message").asText() : "Processing...";
                        jobService.updateProgress(jobId, progress, stepMsg, JobStatus.PROCESSING);
                    } else if ("completed".equals(status)) {
                        JsonNode result = node.get("result");
                        saveRealResults(jobId, result);
                        jobService.updateProgress(jobId, 100, "Completed", JobStatus.DONE);
                        session.close();
                    } else if ("failed".equals(status)) {
                        String error = node.has("error") ? node.get("error").asText() : "Unknown AI error";
                        jobService.updateProgress(jobId, 0, "AI Error: " + error, JobStatus.FAILED);
                        session.close();
                    }
                }
                
                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    jobService.updateProgress(jobId, 0, "WebSocket Error: " + exception.getMessage(), JobStatus.FAILED);
                }
            }, url);
        } catch (Exception e) {
            jobService.updateProgress(jobId, 0, "WebSocket connection failed: " + e.getMessage(), JobStatus.FAILED);
        }
    }

    private void saveRealResults(UUID jobId, JsonNode resultNode) {
        if (!resultNode.has("segments")) return;
        
        for (JsonNode seg : resultNode.get("segments")) {
            TranscriptionSegment segment = new TranscriptionSegment();
            segment.setJobId(jobId);
            segment.setText(seg.get("text").asText().trim());
            segment.setStartTime(seg.get("start").asDouble());
            segment.setEndTime(seg.get("end").asDouble());
            segment.setType(SegmentType.SENTENCE); 
            segmentRepository.save(segment);
            
            if (seg.has("words")) {
                for (JsonNode w : seg.get("words")) {
                    TranscriptionSegment word = new TranscriptionSegment();
                    word.setJobId(jobId);
                    word.setText(w.get("word").asText().trim());
                    word.setStartTime(w.get("start").asDouble());
                    word.setEndTime(w.get("end").asDouble());
                    word.setType(SegmentType.WORD);
                    segmentRepository.save(word);
                }
            }
        }
    }
}

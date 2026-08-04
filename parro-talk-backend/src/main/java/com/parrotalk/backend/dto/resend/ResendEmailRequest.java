package com.parrotalk.backend.dto.resend;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResendEmailRequest {
    
    private String from;
    private List<String> to;
    private String subject;
    private String html;
    private String text;
    
    @JsonProperty("reply_to")
    private String replyTo;
    
    private List<Attachment> attachments;
    private Map<String, String> headers;
    private List<Tag> tags;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Attachment {
        private String filename;
        private String content; // base64 encoded
        private String path; // URL
    }

    @Data
    @Builder
    public static class Tag {
        private String name;
        private String value;
    }
}

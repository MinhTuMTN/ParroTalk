package com.parrotalk.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "email")
@Data
public class EmailProperties {

    private String provider = "resend";
    private String from;
    private String replyTo;
    private Resend resend = new Resend();

    @Data
    public static class Resend {
        private String apiKey = "INSERT_SECRET_HERE";
        private String baseUrl = "https://api.resend.com";
    }
}

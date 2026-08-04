package com.parrotalk.backend.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.parrotalk.backend.config.EmailProperties;
import com.parrotalk.backend.dto.resend.ResendEmailRequest;
import com.parrotalk.backend.dto.resend.ResendEmailResponse;
import com.parrotalk.backend.exception.EmailProviderException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ResendClient {

    private final RestClient restClient;
    private final EmailProperties emailProperties;

    public ResendClient(RestClient.Builder restClientBuilder, EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
        this.restClient = restClientBuilder
                .baseUrl(emailProperties.getResend().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + emailProperties.getResend().getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Retryable(
            retryFor = { EmailProviderException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String sendEmail(ResendEmailRequest request) {
        log.info("Sending email via Resend to [{}] with subject [{}]", request.getTo(), request.getSubject());
        long startTime = System.currentTimeMillis();

        try {
            ResendEmailResponse response = restClient.post()
                    .uri("/emails")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 429) {
                            throw new EmailProviderException("Rate limit exceeded (429)");
                        }
                        throw new EmailProviderException("Client error calling Resend API: " + res.getStatusCode() + " - " + new String(res.getBody().readAllBytes()));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new EmailProviderException("Server error calling Resend API: " + res.getStatusCode());
                    })
                    .body(ResendEmailResponse.class);

            long latency = System.currentTimeMillis() - startTime;
            
            if (response != null && response.getId() != null) {
                log.info("Successfully sent email via Resend to [{}] with Message ID [{}]. Latency: {}ms", request.getTo(), response.getId(), latency);
                return response.getId();
            } else {
                throw new EmailProviderException("No message ID returned from Resend API");
            }
        } catch (RestClientResponseException e) {
            log.error("Failed to send email to [{}]. Status: {}, Response: {}", request.getTo(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmailProviderException("Failed to send email via Resend: " + e.getMessage(), e);
        } catch (EmailProviderException e) {
            log.error("Failed to send email to [{}]. Error: {}", request.getTo(), e.getMessage());
            throw e; // Rethrow to allow retry
        } catch (Exception e) {
            log.error("Unexpected error sending email to [{}]. Error: {}", request.getTo(), e.getMessage());
            throw new EmailProviderException("Unexpected error calling Resend API", e);
        }
    }
}

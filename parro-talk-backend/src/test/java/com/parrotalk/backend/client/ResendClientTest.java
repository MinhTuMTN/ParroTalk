package com.parrotalk.backend.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.parrotalk.backend.config.EmailProperties;
import com.parrotalk.backend.dto.resend.ResendEmailRequest;
import com.parrotalk.backend.exception.EmailProviderException;

class ResendClientTest {

    private ResendClient resendClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        EmailProperties properties = new EmailProperties();
        properties.getResend().setApiKey("test-api-key");
        properties.getResend().setBaseUrl("https://api.resend.com");

        RestClient.Builder builder = RestClient.builder();
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        this.resendClient = new ResendClient(builder, properties);
    }

    @Test
    void shouldSendEmailSuccessfully() {
        ResendEmailRequest request = ResendEmailRequest.builder()
                .from("test@parrotalk.local")
                .to(List.of("user@example.com"))
                .subject("Test Subject")
                .html("<p>Hello</p>")
                .text("Hello")
                .build();

        String responseJson = "{\"id\": \"12345\"}";

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String messageId = resendClient.sendEmail(request);

        assertThat(messageId).isEqualTo("12345");
        mockServer.verify();
    }

    @Test
    void shouldThrowEmailProviderExceptionOn429() {
        ResendEmailRequest request = ResendEmailRequest.builder()
                .from("test@parrotalk.local")
                .to(List.of("user@example.com"))
                .subject("Test Subject")
                .build();

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit"));

        assertThatThrownBy(() -> resendClient.sendEmail(request))
                .isInstanceOf(EmailProviderException.class)
                .hasMessageContaining("Rate limit exceeded");

        mockServer.verify();
    }
    
    @Test
    void shouldThrowEmailProviderExceptionOn500() {
        ResendEmailRequest request = ResendEmailRequest.builder()
                .from("test@parrotalk.local")
                .to(List.of("user@example.com"))
                .subject("Test Subject")
                .build();

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> resendClient.sendEmail(request))
                .isInstanceOf(EmailProviderException.class)
                .hasMessageContaining("Server error calling Resend API: 500");

        mockServer.verify();
    }
}

package com.parrotalk.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;

import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.config.EmailProperties;
import com.parrotalk.backend.constant.EmailType;
import com.parrotalk.backend.dto.VerifyEmailMessage;
import com.parrotalk.backend.dto.resend.ResendEmailRequest;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ResendEmailServiceTest {

    @Mock
    private ResendClient resendClient;

    @Mock
    private EmailTemplateService templateService;

    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private ResendEmailService resendEmailService;

    @BeforeEach
    void setUp() {
        when(emailProperties.getFrom()).thenReturn("Test <test@parrotalk.local>");
        when(emailProperties.getReplyTo()).thenReturn("support@parrotalk.local");
    }

    @Test
    void shouldSendVerificationEmail() {
        // Arrange
        VerifyEmailMessage message = new VerifyEmailMessage("user@example.com", "John Doe", "http://verify", 30L, 1);
        when(templateService.processHtml(eq(EmailType.VERIFY_EMAIL), any(Context.class))).thenReturn("<p>Verify</p>");

        // Act
        resendEmailService.sendVerificationEmail(message);

        // Assert
        ArgumentCaptor<ResendEmailRequest> captor = ArgumentCaptor.forClass(ResendEmailRequest.class);
        verify(resendClient).sendEmail(captor.capture());

        ResendEmailRequest request = captor.getValue();
        assertThat(request.getTo()).containsExactly("user@example.com");
        assertThat(request.getFrom()).isEqualTo("Test <test@parrotalk.local>");
        assertThat(request.getSubject()).contains("Verify your email address");
        assertThat(request.getHtml()).isEqualTo("<p>Verify</p>");
        assertThat(request.getText()).contains("http://verify");
    }

    @Test
    void shouldSendPasswordResetEmail() {
        // Arrange
        when(templateService.processHtml(eq(EmailType.RESET_PASSWORD), any(Context.class))).thenReturn("<p>Reset</p>");

        // Act
        resendEmailService.sendPasswordResetEmail("user@example.com", "http://reset", "John Doe");

        // Assert
        ArgumentCaptor<ResendEmailRequest> captor = ArgumentCaptor.forClass(ResendEmailRequest.class);
        verify(resendClient).sendEmail(captor.capture());

        ResendEmailRequest request = captor.getValue();
        assertThat(request.getTo()).containsExactly("user@example.com");
        assertThat(request.getSubject()).contains("Password Reset Request");
        assertThat(request.getHtml()).isEqualTo("<p>Reset</p>");
        assertThat(request.getText()).contains("http://reset");
    }
}

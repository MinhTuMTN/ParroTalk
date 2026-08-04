package com.parrotalk.backend.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.parrotalk.backend.config.EmailProperties;
import com.parrotalk.backend.constant.EmailType;
import com.parrotalk.backend.exception.EmailTemplateException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final SpringTemplateEngine templateEngine;
    private final EmailProperties emailProperties;

    public String processHtml(EmailType type, Context context) {
        try {
            String templateName = getTemplateName(type);
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            throw new EmailTemplateException("Failed to process HTML template for type: " + type, e);
        }
    }

    private String getTemplateName(EmailType type) {
        return switch (type) {
            case VERIFY_EMAIL -> "email/verify-email";
            case RESET_PASSWORD -> "email/reset-password";
            case WELCOME -> "email/welcome";
            case CHANGE_EMAIL -> "email/change-email";
            case NOTIFICATION -> "email/notification";
            case SYSTEM -> "email/system";
        };
    }
}

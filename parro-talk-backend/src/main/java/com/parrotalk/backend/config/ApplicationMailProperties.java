package com.parrotalk.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Application-wide mail-related properties.
 * 
 * @author MinhTuMTN
 */
@Component
@ConfigurationProperties(prefix = "application.mail")
@Data
public class ApplicationMailProperties {

    /** Application name */
    private String appName = "ParroTalk";

    /** Support email */
    private String supportEmail;
}

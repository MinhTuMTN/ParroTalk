package com.parrotalk.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Application setting.
 * Used to store application settings.
 * 
 * @author MinhTuMTN
 */
@Component
@ConfigurationProperties(prefix = "settings")
@Data
public class AppSetting {

    /** Default audio thumbnail url. */
    private String defaultAudioThumbnail;

    /** Default application time zone used for learner-facing daily calculations. */
    private String defaultTimeZone = "Asia/Bangkok";
}

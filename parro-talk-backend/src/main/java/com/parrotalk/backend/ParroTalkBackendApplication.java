package com.parrotalk.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ParroTalk application.
 * 
 * @author MinhTuMTN
 */
@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "com.parrotalk.backend.repository")
public class ParroTalkBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParroTalkBackendApplication.class, args);
    }

}

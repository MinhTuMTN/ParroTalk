package com.parrotalk.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * ParroTalk application.
 * 
 * @author MinhTuMTN
 */
@SpringBootApplication
@EnableCaching
public class ParroTalkBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParroTalkBackendApplication.class, args);
    }

}

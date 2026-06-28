package com.parrotalk.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health Check Controller.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    /**
     * Health check endpoint.
     * 
     * @return "OK" string if the service is healthy
     */
    @GetMapping
    public String healthCheck() {
        return "OK";
    }
}

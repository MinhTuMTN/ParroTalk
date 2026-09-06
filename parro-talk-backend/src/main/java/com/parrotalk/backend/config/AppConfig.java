package com.parrotalk.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Application configuration class that sets up CORS, authentication, and
 * other beans required by the application.
 * 
 * @author MinhTuMTN
 */
@Configuration
@EnableAsync
@EnableRetry
@RequiredArgsConstructor
public class AppConfig implements WebMvcConfigurer {

    /** Frontend properties to configure CORS mappings. */
    private final FrontendProperties frontendProperties;

    /**
     * Configures Cross-Origin Resource Sharing (CORS) mappings to allow
     * cross-origin requests from specified origins.
     *
     * @param registry the {@link CorsRegistry} to configure CORS mappings for
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = frontendProperties.getAllowedOrigins() != null
                ? frontendProperties.getAllowedOrigins().toArray(String[]::new)
                : new String[0];
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates a {@link RestClient.Builder} with custom timeouts.
     * Used for making HTTP requests to other services (Resend,...)
     * 
     * @return the {@link RestClient.Builder} with custom timeouts
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * Dedicated executor for blocking database operations.
     * Prevents blocking queries from exhausting the ForkJoinPool common pool.
     */
    @Bean(name = "dbQueryExecutor")
    public Executor dbQueryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("db-query-");
        executor.initialize();
        return executor;
    }

    /**
     * Default primary task executor for asynchronous processing and general application tasks.
     */
    @Primary
    @Bean(name = { "applicationTaskExecutor", "taskExecutor" })
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("app-task-");
        executor.initialize();
        return executor;
    }
}

package com.parrotalk.backend.config;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson2.CoreJackson2Module;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parrotalk.backend.constant.CacheNames;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis cache config.
 * 
 * @author MinhTuMTN
 */
@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

    private static final Map<String, Duration> CACHE_TTLS = Map.of(
            CacheNames.LESSON_SEARCH_CACHE,
            Duration.ofMinutes(5),
            CacheNames.LESSON_DETAIL_CACHE,
            Duration.ofHours(1),
            CacheNames.FEATURED_LESSONS_CACHE,
            Duration.ofMinutes(30),
            CacheNames.USER_CACHE,
            Duration.ofHours(2),
            CacheNames.ADMIN_USER_DETAIL_CACHE,
            Duration.ofMinutes(15),
            CacheNames.LIBRARY_SECTIONS_CACHE,
            Duration.ofMinutes(5));

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper baseMapper) {
        ObjectMapper redisMapper = baseMapper.copy();
        redisMapper.registerModule(new CoreJackson2Module());
        redisMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisMapper);

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30))
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                        .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = CACHE_TTLS.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> defaultConfig.entryTtl(entry.getValue())));

        return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs).build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Failed to get value from cache '{}' with key '{}'. Evicting key and falling back to database. Error: {}",
                        cache.getName(), key, exception.getMessage());
                try {
                    cache.evict(key);
                } catch (Exception e) {
                    log.error("Failed to evict corrupted cache key '{}'", key, e);
                }
            }
        };
    }
}

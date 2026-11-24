package com.parrotalk.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.security.jackson2.CoreJackson2Module;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.entity.User;

class CacheConfigUserSerializationTest {

    @Test
    void userRoundTripThroughCacheConfigSerializer() {
        ObjectMapper baseMapper = new ObjectMapper();
        baseMapper.registerModule(new JavaTimeModule());
        baseMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ObjectMapper redisMapper = baseMapper.copy();
        redisMapper.registerModule(new CoreJackson2Module());
        redisMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisMapper);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("learner@example.com")
                .password("hashed")
                .role(Role.USER)
                .displayUsername("learner")
                .fullName("Learner Name")
                .emailVerified(true)
                .enabled(true)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        byte[] bytes = serializer.serialize(user);
        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        User restored = (User) deserialized;
        assertEquals(user.getId(), restored.getId());
        assertEquals(user.getEmail(), restored.getEmail());
    }
}

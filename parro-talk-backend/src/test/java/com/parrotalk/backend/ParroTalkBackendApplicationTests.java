package com.parrotalk.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.parrotalk.backend.client.ResendClient;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@ActiveProfiles("test")
class ParroTalkBackendApplicationTests {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

	@Test
	void contextLoads() {
	}

}

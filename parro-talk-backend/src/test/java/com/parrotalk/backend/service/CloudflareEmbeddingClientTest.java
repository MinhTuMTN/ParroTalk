package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.parrotalk.backend.config.CloudflareProperties;
import com.parrotalk.backend.constant.CacheNames;

class CloudflareEmbeddingClientTest {

    private CloudflareProperties properties;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        properties = new CloudflareProperties();
        properties.setEnabled(true);
        properties.setAccountId("acc-123");
        properties.setApiToken("tok-secret");
        properties.setEmbeddingModel("@cf/baai/bge-m3");
        properties.setExpectedDimensions(1024);

        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        cacheManager = new ConcurrentMapCacheManager(CacheNames.SEMANTIC_QUERY_EMBEDDING_CACHE);
    }

    private ObjectProvider<CacheManager> providerOf(CacheManager manager) {
        return new ObjectProvider<>() {
            @Override
            public CacheManager getObject(Object... args) {
                return manager;
            }

            @Override
            public CacheManager getIfAvailable() {
                return manager;
            }

            @Override
            public CacheManager getIfUnique() {
                return manager;
            }

            @Override
            public CacheManager getObject() {
                return manager;
            }
        };
    }

    @Test
    void embedQueryToVectorLiteral_returnsEmptyWhenUnconfigured() {
        properties.setApiToken("");
        CloudflareEmbeddingClient client = new CloudflareEmbeddingClient(
                RestClient.builder(), properties, providerOf(null));

        assertTrue(client.embedQueryToVectorLiteral("công thức nấu ăn").isEmpty());
        assertTrue(client.embedQueryToVectorLiteral("   ").isEmpty());
    }

    @Test
    void embedQueryToVectorLiteral_callsCloudflareFormatsVectorAndCaches() {
        String vectorJson = Collections.nCopies(1024, "0.25").stream()
                .collect(Collectors.joining(",", "[", "]"));
        String body = "{\"success\":true,\"result\":{\"data\":[" + vectorJson + "]}}";

        mockServer.expect(requestTo("https://api.cloudflare.com/client/v4/accounts/acc-123/ai/run/@cf/baai/bge-m3"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-secret"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CloudflareEmbeddingClient client = new CloudflareEmbeddingClient(
                restClientBuilder, properties, providerOf(cacheManager));

        Optional<String> first = client.embedQueryToVectorLiteral("  Atomic   Bomb ");
        Optional<String> second = client.embedQueryToVectorLiteral("atomic bomb");

        assertTrue(first.isPresent());
        assertEquals(vectorJson, first.get());
        assertEquals(first, second);
        mockServer.verify();
    }

    @Test
    void embedQueryToVectorLiteral_returnsEmptyOnWrongDimsOrServerError() {
        String badVectorJson = Collections.nCopies(768, "0.1").stream()
                .collect(Collectors.joining(",", "[", "]"));
        String badBody = "{\"success\":true,\"result\":{\"data\":[" + badVectorJson + "]}}";

        mockServer.expect(requestTo("https://api.cloudflare.com/client/v4/accounts/acc-123/ai/run/@cf/baai/bge-m3"))
                .andRespond(withSuccess(badBody, MediaType.APPLICATION_JSON));

        CloudflareEmbeddingClient client = new CloudflareEmbeddingClient(
                restClientBuilder, properties, providerOf(null));

        assertTrue(client.embedQueryToVectorLiteral("query 1").isEmpty());
        mockServer.verify();

        mockServer.reset();
        mockServer.expect(requestTo("https://api.cloudflare.com/client/v4/accounts/acc-123/ai/run/@cf/baai/bge-m3"))
                .andRespond(withServerError());

        assertTrue(client.embedQueryToVectorLiteral("query 2").isEmpty());
        mockServer.verify();
    }
}

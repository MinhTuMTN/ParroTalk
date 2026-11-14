package com.parrotalk.backend.service;

import com.parrotalk.backend.exception.ParroTalkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto.WritingCompleteSentenceDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto.WritingLexicalItemDto;
import com.parrotalk.backend.entity.TtsAudioCache;
import com.parrotalk.backend.repository.TtsAudioCacheRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WritingTtsServiceTest {

    @TempDir
    Path tempDir;

    private WritingTtsService ttsService;

    @BeforeEach
    void setUp() {
        ttsService = new WritingTtsService(tempDir.toString(), "python", 8L);
    }

    @Test
    @DisplayName("normalizeAccent maps UK/GB variants to UK and defaults everything else to US")
    void testNormalizeAccent() {
        assertEquals("UK", ttsService.normalizeAccent("UK"));
        assertEquals("UK", ttsService.normalizeAccent(" uk "));
        assertEquals("UK", ttsService.normalizeAccent("GB"));
        assertEquals("UK", ttsService.normalizeAccent("en-gb"));
        assertEquals("UK", ttsService.normalizeAccent("EN_GB"));

        assertEquals("US", ttsService.normalizeAccent(null));
        assertEquals("US", ttsService.normalizeAccent(""));
        assertEquals("US", ttsService.normalizeAccent("US"));
        assertEquals("US", ttsService.normalizeAccent("en-US"));
        assertEquals("US", ttsService.normalizeAccent("AU"));
    }

    @Test
    @DisplayName("resolveVoice returns UK and US Edge-TTS voices")
    void testResolveVoice() {
        assertEquals(WritingTtsService.VOICE_UK, ttsService.resolveVoice("UK"));
        assertEquals(WritingTtsService.VOICE_UK, ttsService.resolveVoice("uk"));
        assertEquals(WritingTtsService.VOICE_US, ttsService.resolveVoice("US"));
        assertEquals(WritingTtsService.VOICE_US, ttsService.resolveVoice(null));
    }

    @Test
    @DisplayName("resolveVoice(accent, voice) returns canonical allowlisted voice or falls back to accent default")
    void testResolveVoice_WithRequestedVoice() {
        assertEquals("en-US-AriaNeural", ttsService.resolveVoice("US", "en-us-ariaNeural"));
        assertEquals("en-US-GuyNeural", ttsService.resolveVoice("US", " en-US-GuyNeural "));
        assertEquals("en-GB-RyanNeural", ttsService.resolveVoice("UK", "EN-GB-RYANNEURAL"));
        assertEquals("en-GB-LibbyNeural", ttsService.resolveVoice("UK", "en-gb-libbyneural"));

        // Blank, unknown, or wrong-accent voices fall back safely to accent default
        assertEquals(WritingTtsService.VOICE_US, ttsService.resolveVoice("US", null));
        assertEquals(WritingTtsService.VOICE_US, ttsService.resolveVoice("US", "  "));
        assertEquals(WritingTtsService.VOICE_US, ttsService.resolveVoice("US", "unknown-voice"));
        assertEquals(WritingTtsService.VOICE_US, ttsService.resolveVoice("US", "en-GB-RyanNeural"));
        assertEquals(WritingTtsService.VOICE_UK, ttsService.resolveVoice("UK", "en-US-AriaNeural"));
    }

    @Test
    @DisplayName("computeCacheFileName with voice shares default voice key and isolates non-default voices")
    void testComputeCacheFileName_WithVoice() {
        String defaultUs = ttsService.computeCacheFileName("hello", "US");
        String explicitDefaultUs = ttsService.computeCacheFileName("hello", "US", WritingTtsService.VOICE_US);
        String guyUs = ttsService.computeCacheFileName("hello", "US", "en-US-GuyNeural");
        String ariaUs = ttsService.computeCacheFileName("hello", "US", "en-US-AriaNeural");

        assertEquals(defaultUs, explicitDefaultUs);
        assertNotEquals(defaultUs, guyUs);
        assertNotEquals(guyUs, ariaUs);
        assertTrue(guyUs.startsWith("us_"));
        assertTrue(guyUs.endsWith(".mp3"));

        String defaultUk = ttsService.computeCacheFileName("hello", "UK");
        String ryanUk = ttsService.computeCacheFileName("hello", "UK", "en-GB-RyanNeural");
        assertEquals(defaultUk, ttsService.computeCacheFileName("hello", "UK", WritingTtsService.VOICE_UK));
        assertNotEquals(defaultUk, ryanUk);
        assertTrue(ryanUk.startsWith("uk_"));
    }

    @Test
    @DisplayName("synthesize(text, accent, voice) uses canonical requested voice and caches separately from default voice")
    void testSynthesize_WithCustomVoice_CachesSeparately() throws Exception {
        WritingTtsService spyService = spy(ttsService);
        byte[] ryanBytes = "ID3-ryan-mp3".getBytes(StandardCharsets.UTF_8);

        doAnswer(inv -> {
            Path targetFile = inv.getArgument(2);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, ryanBytes);
            return ryanBytes;
        }).when(spyService).runEdgeTtsProcess(eq("vocabulary"), eq("en-GB-RyanNeural"), any(Path.class));

        byte[] first = spyService.synthesize("vocabulary", "UK", "en-gb-ryanneural");
        byte[] second = spyService.synthesize("vocabulary", "UK", "en-GB-RyanNeural");

        assertArrayEquals(ryanBytes, first);
        assertArrayEquals(ryanBytes, second);
        verify(spyService, times(1)).runEdgeTtsProcess(eq("vocabulary"), eq("en-GB-RyanNeural"), any(Path.class));
    }

    @Test
    @DisplayName("cleanAndValidateText strips control characters, collapses whitespace, and enforces bounds")
    void testCleanAndValidateText() {
        String dirty = "  hello\u0000\u0007\t\n  world\u001f!  ";
        assertEquals("hello world!", ttsService.cleanAndValidateText(dirty));

        String exactLimit = "a".repeat(WritingTtsService.MAX_TEXT_LENGTH);
        assertEquals(exactLimit, ttsService.cleanAndValidateText(exactLimit));

        ParroTalkException nullEx = assertThrows(ParroTalkException.class, () -> ttsService.cleanAndValidateText(null));
        assertEquals(HttpStatus.BAD_REQUEST, nullEx.getHttpStatusCode());

        ParroTalkException blankEx = assertThrows(ParroTalkException.class,
                () -> ttsService.cleanAndValidateText(" \u0000\t\n "));
        assertEquals(HttpStatus.BAD_REQUEST, blankEx.getHttpStatusCode());

        String tooLong = "a".repeat(WritingTtsService.MAX_TEXT_LENGTH + 1);
        ParroTalkException lengthEx = assertThrows(ParroTalkException.class,
                () -> ttsService.cleanAndValidateText(tooLong));
        assertEquals(HttpStatus.BAD_REQUEST, lengthEx.getHttpStatusCode());
    }

    @Test
    @DisplayName("buildTtsEndpointUrl encodes query params cleanly and returns null when invalid")
    void testBuildTtsEndpointUrl() {
        assertNull(ttsService.buildTtsEndpointUrl(null, "US"));
        assertNull(ttsService.buildTtsEndpointUrl("   ", "UK"));
        assertNull(ttsService.buildTtsEndpointUrl("a".repeat(WritingTtsService.MAX_TEXT_LENGTH + 1), "US"));

        String usUrl = ttsService.buildTtsEndpointUrl("  blood\t alcohol  ", "en-us");
        assertEquals("/api/writing/tts?text=blood+alcohol&accent=US", usUrl);

        String ukUrl = ttsService.buildTtsEndpointUrl("impair", "gb");
        assertEquals("/api/writing/tts?text=impair&accent=UK", ukUrl);
    }

    @Test
    @DisplayName("computeCacheFileName is deterministic, accent-scoped, and preserves text case sensitivity")
    void testComputeCacheFileName() {
        String usAcronym = ttsService.computeCacheFileName("US", "US");
        String usPronoun = ttsService.computeCacheFileName("us", "US");
        String itAcronym = ttsService.computeCacheFileName("IT", "US");
        String itPronoun = ttsService.computeCacheFileName("it", "US");
        String ukWord = ttsService.computeCacheFileName("impair", "UK");
        String usWord = ttsService.computeCacheFileName("impair", "US");

        assertEquals(usAcronym, ttsService.computeCacheFileName("US", "US"));
        assertNotEquals(usAcronym, usPronoun);
        assertNotEquals(itAcronym, itPronoun);
        assertNotEquals(usWord, ukWord);
        assertTrue(usWord.startsWith("us_"));
        assertTrue(ukWord.startsWith("uk_"));
        assertTrue(usWord.endsWith(".mp3"));
        assertEquals(3 + 64 + 4, usWord.length());
    }

    @Test
    @DisplayName("synthesize serves cached MP3 from disk without invoking runEdgeTtsProcess")
    void testSynthesize_CacheHit_SkipsProcess() throws Exception {
        String cleaned = "impair";
        String fileName = ttsService.computeCacheFileName(cleaned, "UK");
        byte[] expectedBytes = "ID3-cached-mp3".getBytes(StandardCharsets.UTF_8);
        Files.write(tempDir.resolve(fileName), expectedBytes);

        WritingTtsService spyService = spy(ttsService);

        byte[] result = spyService.synthesize("  impair  ", "en-gb");

        assertArrayEquals(expectedBytes, result);
        verify(spyService, never()).runEdgeTtsProcess(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("synthesize invokes runEdgeTtsProcess on cache miss and caches result on disk")
    void testSynthesize_CacheMiss_CallsProcessAndUsesDiskCacheNext() throws Exception {
        WritingTtsService spyService = spy(ttsService);
        byte[] generatedBytes = "ID3-generated-mp3".getBytes(StandardCharsets.UTF_8);

        doAnswer(inv -> {
            Path targetFile = inv.getArgument(2);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, generatedBytes);
            return generatedBytes;
        }).when(spyService).runEdgeTtsProcess(eq("drive safely"), eq(WritingTtsService.VOICE_US), any(Path.class));

        byte[] first = spyService.synthesize("drive   safely", "US");
        byte[] second = spyService.synthesize("drive safely", "US");

        assertArrayEquals(generatedBytes, first);
        assertArrayEquals(generatedBytes, second);
        verify(spyService, times(1)).runEdgeTtsProcess(eq("drive safely"), eq(WritingTtsService.VOICE_US), any(Path.class));
    }

    @Test
    @DisplayName("synthesize deduplicates concurrent identical requests with single-flight execution")
    void testSynthesize_SingleFlightDeduplication() throws Exception {
        WritingTtsService spyService = spy(ttsService);
        byte[] generatedBytes = "ID3-single-flight-mp3".getBytes(StandardCharsets.UTF_8);
        AtomicInteger processCalls = new AtomicInteger(0);
        CountDownLatch leaderEntered = new CountDownLatch(1);
        CountDownLatch releaseLeader = new CountDownLatch(1);

        doAnswer(inv -> {
            processCalls.incrementAndGet();
            leaderEntered.countDown();
            assertTrue(releaseLeader.await(5, TimeUnit.SECONDS));
            Path targetFile = inv.getArgument(2);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, generatedBytes);
            return generatedBytes;
        }).when(spyService).runEdgeTtsProcess(eq("concurrent phrase"), eq(WritingTtsService.VOICE_UK), any(Path.class));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<byte[]> task = () -> spyService.synthesize("concurrent phrase", "UK");
            Future<byte[]> f1 = pool.submit(task);
            assertTrue(leaderEntered.await(5, TimeUnit.SECONDS));
            Future<byte[]> f2 = pool.submit(task);

            releaseLeader.countDown();

            assertArrayEquals(generatedBytes, f1.get(5, TimeUnit.SECONDS));
            assertArrayEquals(generatedBytes, f2.get(5, TimeUnit.SECONDS));
            assertEquals(1, processCalls.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("synthesize cleans up in-flight state on failure so subsequent calls can retry and succeed")
    void testSynthesize_CleansUpInFlightAfterFailure() {
        WritingTtsService spyService = spy(ttsService);
        byte[] recoveredBytes = "ID3-recovered".getBytes(StandardCharsets.UTF_8);

        doThrow(new RuntimeException("transient failure"))
                .doReturn(recoveredBytes)
                .when(spyService).runEdgeTtsProcess(eq("retry phrase"), eq(WritingTtsService.VOICE_US), any(Path.class));

        assertThrows(RuntimeException.class, () -> spyService.synthesize("retry phrase", "US"));

        byte[] secondAttempt = spyService.synthesize("retry phrase", "US");
        assertArrayEquals(recoveredBytes, secondAttempt);
        verify(spyService, times(2)).runEdgeTtsProcess(eq("retry phrase"), eq(WritingTtsService.VOICE_US), any(Path.class));
    }

    @Test
    @DisplayName("runEdgeTtsProcess throws ParroTalkException when executable cannot be started")
    void testRunEdgeTtsProcess_InvalidExecutableThrows() {
        WritingTtsService badCmdService = new WritingTtsService(tempDir.toString(), "nonexistent-python-cmd-xyz", 5L);
        Path target = tempDir.resolve("us_invalid.mp3");

        ParroTalkException ex = assertThrows(ParroTalkException.class,
                () -> badCmdService.runEdgeTtsProcess("hello", WritingTtsService.VOICE_US, target));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatusCode());
        assertFalse(Files.exists(target));
    }

    @Test
    @DisplayName("synthesize enforces bounded Semaphore concurrency limit and releases permits on completion and error")
    void testSynthesize_SemaphoreLimitsConcurrencyAndReleasesPermits() throws Exception {
        WritingTtsService boundedService = spy(new WritingTtsService(tempDir.toString(), "python", 5L, 1));
        assertEquals(1, boundedService.availableProcessPermits());

        AtomicInteger activeProcesses = new AtomicInteger(0);
        AtomicInteger maxObservedActive = new AtomicInteger(0);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        doAnswer(inv -> {
            int active = activeProcesses.incrementAndGet();
            maxObservedActive.updateAndGet(prev -> Math.max(prev, active));
            try {
                if ("phrase one".equals(inv.getArgument(0))) {
                    firstEntered.countDown();
                    assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
                }
                return ("audio-" + inv.getArgument(0)).getBytes(StandardCharsets.UTF_8);
            } finally {
                activeProcesses.decrementAndGet();
            }
        }).when(boundedService).runEdgeTtsProcess(anyString(), anyString(), any(Path.class));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<byte[]> f1 = pool.submit(() -> boundedService.synthesize("phrase one", "US"));
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            assertEquals(0, boundedService.availableProcessPermits());

            Future<byte[]> f2 = pool.submit(() -> boundedService.synthesize("phrase two", "US"));
            // Wait briefly to confirm f2 is blocked on semaphore while f1 runs
            Thread.sleep(100);
            assertEquals(1, maxObservedActive.get());
            assertFalse(f2.isDone());

            releaseFirst.countDown();

            assertArrayEquals("audio-phrase one".getBytes(StandardCharsets.UTF_8), f1.get(5, TimeUnit.SECONDS));
            assertArrayEquals("audio-phrase two".getBytes(StandardCharsets.UTF_8), f2.get(5, TimeUnit.SECONDS));
            assertEquals(1, maxObservedActive.get());
            assertEquals(1, boundedService.availableProcessPermits());
        } finally {
            pool.shutdownNow();
        }

        // Verify permit is also released after a synthesis error
        doThrow(new RuntimeException("boom"))
                .when(boundedService).runEdgeTtsProcess(eq("failing phrase"), anyString(), any(Path.class));
        assertThrows(RuntimeException.class, () -> boundedService.synthesize("failing phrase", "US"));
        assertEquals(1, boundedService.availableProcessPermits());
    }

    @Test
    @DisplayName("synthesize throws 503 SERVICE_UNAVAILABLE when semaphore permit acquisition times out")
    void testSynthesize_SemaphoreTimeoutReturns503() throws Exception {
        WritingTtsService busyService = spy(new WritingTtsService(tempDir.toString(), "python", 1L, 1));
        CountDownLatch holderEntered = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);

        doAnswer(inv -> {
            if ("slow phrase".equals(inv.getArgument(0))) {
                holderEntered.countDown();
                assertTrue(releaseHolder.await(5, TimeUnit.SECONDS));
            }
            return "audio-ok".getBytes(StandardCharsets.UTF_8);
        }).when(busyService).runEdgeTtsProcess(anyString(), anyString(), any(Path.class));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<byte[]> holderFuture = pool.submit(() -> busyService.synthesize("slow phrase", "US"));
            assertTrue(holderEntered.await(5, TimeUnit.SECONDS));

            ParroTalkException busyEx = assertThrows(ParroTalkException.class,
                    () -> busyService.synthesize("second phrase", "US"));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, busyEx.getHttpStatusCode());

            releaseHolder.countDown();
            assertArrayEquals("audio-ok".getBytes(StandardCharsets.UTF_8), holderFuture.get(5, TimeUnit.SECONDS));
            assertEquals(1, busyService.availableProcessPermits());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("resolveCachedFileIfExists returns Optional path only when non-empty cached MP3 exists")
    void testResolveCachedFileIfExists() throws Exception {
        assertTrue(ttsService.resolveCachedFileIfExists("impair", "US", null).isEmpty());
        assertTrue(ttsService.resolveCachedFileIfExists("   ", "US", null).isEmpty());

        String fileName = ttsService.computeCacheFileName("impair", "US");
        Path cacheFile = tempDir.resolve(fileName);
        Files.write(cacheFile, new byte[0]);
        assertTrue(ttsService.resolveCachedFileIfExists("impair", "US", null).isEmpty());

        Files.write(cacheFile, "MP3-DATA".getBytes(StandardCharsets.UTF_8));
        Optional<Path> resolved = ttsService.resolveCachedFileIfExists("  impair ", "en-US", "  ");
        assertTrue(resolved.isPresent());
        assertEquals(cacheFile, resolved.get());
    }

    @Test
    @DisplayName("TtsPrewarmService submits prioritized clips, skips dictionary URLs, respects maxClips, and swallows failures")
    void testTtsPrewarmService_PriorityCapSkipDictAndSwallowFailures() throws Exception {
        WritingTtsService ttsMock = mock(WritingTtsService.class);
        List<String> callOrder = new ArrayList<>();
        when(ttsMock.synthesize(anyString(), anyString())).thenAnswer(inv -> {
            String text = inv.getArgument(0);
            String accent = inv.getArgument(1);
            synchronized (callOrder) {
                callOrder.add(accent + ":" + text);
            }
            if ("boom word".equals(text)) {
                throw new RuntimeException("edge-tts transient failure");
            }
            return new byte[]{1, 2, 3};
        });

        WritingSessionResponseDto session = WritingSessionResponseDto.builder()
                .vocabulary(List.of(
                        WritingLexicalItemDto.builder()
                                .text("resilient")
                                .audioUsUrl("https://dict.example.com/resilient-us.mp3")
                                .audioUkUrl("/api/writing/tts?text=resilient&accent=UK")
                                .exampleEn("Resilient people recover quickly.")
                                .exampleAudioUsUrl("/api/writing/tts?text=ex&accent=US")
                                .exampleAudioUkUrl("/api/writing/tts?text=ex&accent=UK")
                                .build(),
                        WritingLexicalItemDto.builder()
                                .text("boom word")
                                .audioUsUrl("/api/writing/tts?text=boom+word&accent=US")
                                .audioUkUrl("/api/writing/tts?text=boom+word&accent=UK")
                                .build()
                ))
                .expressions(List.of(
                        WritingLexicalItemDto.builder()
                                .text("bounce back")
                                .audioUsUrl("/api/writing/tts?text=bounce+back&accent=US")
                                .audioUkUrl("/api/writing/tts?text=bounce+back&accent=UK")
                                .build()
                ))
                .collocations(List.of(
                        WritingLexicalItemDto.builder()
                                .text("stay resilient")
                                .audioUsUrl("/api/writing/tts?text=stay+resilient&accent=US")
                                .audioUkUrl("/api/writing/tts?text=stay+resilient&accent=UK")
                                .build()
                ))
                .completeSentences(List.of(
                        WritingCompleteSentenceDto.builder()
                                .sentenceEn("Resilient learners bounce back.")
                                .audioUsUrl("/api/writing/tts?text=s&accent=US")
                                .audioUkUrl("/api/writing/tts?text=s&accent=UK")
                                .build()
                ))
                .build();

        ExecutorService singleThread = Executors.newSingleThreadExecutor();
        TtsPrewarmService prewarm = new TtsPrewarmService(ttsMock, true, 7, singleThread);
        try {
            assertDoesNotThrow(() -> prewarm.prewarmSession(session));
        } finally {
            singleThread.shutdown();
            assertTrue(singleThread.awaitTermination(5, TimeUnit.SECONDS));
        }

        // Total possible non-dict clips = 1(resilient UK) + 2(boom word) + 2(bounce back) + 2(stay resilient) + 2(example) + 2(sentence) = 11
        // Capped at maxClips = 7 in strict priority order:
        assertEquals(List.of(
                "UK:resilient",
                "US:boom word",
                "UK:boom word",
                "US:bounce back",
                "UK:bounce back",
                "US:stay resilient",
                "UK:stay resilient"
        ), callOrder);

        // Disabled prewarm submits nothing
        WritingTtsService disabledTtsMock = mock(WritingTtsService.class);
        TtsPrewarmService disabledPrewarm = new TtsPrewarmService(disabledTtsMock, false, 40);
        disabledPrewarm.prewarmSession(session);
        disabledPrewarm.shutdown();
        verifyNoInteractions(disabledTtsMock);
    }

    @Test
    @DisplayName("resolveCloudinaryUrlIfCached returns stored URL on hit and empty on miss")
    void testResolveCloudinaryUrlIfCached() {
        TtsAudioCacheRepository repoMock = mock(TtsAudioCacheRepository.class);
        ttsService.setTtsAudioCacheRepository(repoMock);

        String expectedKey = ttsService.computeCacheKey("resilient", "US", null);
        when(repoMock.findById(expectedKey)).thenReturn(Optional.of(
                TtsAudioCache.builder()
                        .cacheKey(expectedKey)
                        .cloudinaryUrl("https://res.cloudinary.com/demo/video/upload/tts/" + expectedKey + ".mp3")
                        .accent("US")
                        .voice(WritingTtsService.VOICE_US)
                        .build()
        ));

        Optional<String> hit = ttsService.resolveCloudinaryUrlIfCached("  resilient  ", "US", null);
        assertTrue(hit.isPresent());
        assertTrue(hit.get().endsWith(expectedKey + ".mp3"));

        Optional<String> miss = ttsService.resolveCloudinaryUrlIfCached("uncached word", "UK", null);
        assertTrue(miss.isEmpty());
    }

    @Test
    @DisplayName("TtsPrewarmService publishes only uncached TTS items to RabbitMQ when prewarmMode=rabbitmq")
    @SuppressWarnings("unchecked")
    void testTtsPrewarmService_RabbitMqPublishesUncachedItems() {
        RabbitTemplate rabbitMock = mock(RabbitTemplate.class);
        TtsAudioCacheRepository repoMock = mock(TtsAudioCacheRepository.class);
        WritingTtsService realTts = new WritingTtsService(tempDir.toString(), "python", 8L);

        String cachedKey = realTts.computeCacheKey("resilient", "US", null);
        when(repoMock.findAllByCacheKeyIn(any())).thenReturn(List.of(
                TtsAudioCache.builder()
                        .cacheKey(cachedKey)
                        .cloudinaryUrl("https://res.cloudinary.com/demo/video/upload/tts/" + cachedKey + ".mp3")
                        .accent("US")
                        .voice(WritingTtsService.VOICE_US)
                        .build()
        ));

        TtsPrewarmService prewarm = new TtsPrewarmService(realTts, true, 10);
        prewarm.setRabbitTemplate(rabbitMock);
        prewarm.setTtsAudioCacheRepository(repoMock);
        prewarm.setPrewarmMode("rabbitmq");
        try {
            WritingSessionResponseDto session = WritingSessionResponseDto.builder()
                    .vocabulary(List.of(
                            WritingLexicalItemDto.builder()
                                    .text("resilient")
                                    .audioUsUrl("/api/writing/tts?text=resilient&accent=US")
                                    .audioUkUrl("/api/writing/tts?text=resilient&accent=UK")
                                    .build()
                    ))
                    .build();

            prewarm.prewarmSession(session);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(rabbitMock, times(1)).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE),
                    eq(RabbitMQConfig.TTS_SYNTHESIS_QUEUE),
                    payloadCaptor.capture()
            );

            java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadCaptor.getValue();
            List<java.util.Map<String, String>> items = (List<java.util.Map<String, String>>) payload.get("items");
            assertEquals(1, items.size());
            assertEquals("resilient", items.get(0).get("text"));
            assertEquals("UK", items.get(0).get("accent"));
            assertEquals(WritingTtsService.VOICE_UK, items.get(0).get("voice"));
        } finally {
            prewarm.shutdown();
        }
    }
}

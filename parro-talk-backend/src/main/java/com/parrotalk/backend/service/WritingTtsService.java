package com.parrotalk.backend.service;

import com.parrotalk.backend.constant.ErrorCode;
import com.parrotalk.backend.entity.TtsAudioCache;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.TtsAudioCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Service for synthesizing US/UK pronunciation audio via Edge-TTS
 * with persistent disk caching and single-flight request deduplication.
 */
@Service
@Slf4j
public class WritingTtsService {

    public static final String VOICE_US = "en-US-JennyNeural";
    public static final String VOICE_UK = "en-GB-SoniaNeural";
    public static final List<String> ALLOWED_US_VOICES = List.of(
            VOICE_US, "en-US-GuyNeural", "en-US-AriaNeural"
    );
    public static final List<String> ALLOWED_UK_VOICES = List.of(
            VOICE_UK, "en-GB-RyanNeural", "en-GB-LibbyNeural"
    );
    public static final int MAX_TEXT_LENGTH = 500;

    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]");

    private final String ttsCacheDir;
    private final String pythonCommand;
    private final long timeoutSeconds;
    private final Semaphore processSemaphore;
    private final ConcurrentHashMap<String, CompletableFuture<byte[]>> inFlightRequests = new ConcurrentHashMap<>();
    private TtsAudioCacheRepository ttsAudioCacheRepository;

    @Autowired(required = false)
    void setTtsAudioCacheRepository(TtsAudioCacheRepository ttsAudioCacheRepository) {
        this.ttsAudioCacheRepository = ttsAudioCacheRepository;
    }

    WritingTtsService() {
        this("uploads/tts", "python", 8L, 4);
    }

    public WritingTtsService(String ttsCacheDir, String pythonCommand, long timeoutSeconds) {
        this(ttsCacheDir, pythonCommand, timeoutSeconds, 4);
    }

    @Autowired
    public WritingTtsService(
            @Value("${app.tts.cache-dir:uploads/tts}") String ttsCacheDir,
            @Value("${app.tts.python-command:python}") String pythonCommand,
            @Value("${app.tts.timeout-seconds:8}") long timeoutSeconds,
            @Value("${app.tts.max-concurrent-processes:4}") int maxConcurrentProcesses) {
        this.ttsCacheDir = ttsCacheDir;
        this.pythonCommand = pythonCommand;
        this.timeoutSeconds = timeoutSeconds;
        this.processSemaphore = new Semaphore(Math.max(1, maxConcurrentProcesses), true);
    }

    /**
     * Normalizes accent input to "UK" or "US" (defaulting to "US").
     */
    public String normalizeAccent(String accent) {
        if (accent == null) {
            return "US";
        }
        String normalized = accent.trim().toUpperCase(Locale.ROOT);
        if ("UK".equals(normalized)
                || "GB".equals(normalized)
                || "EN-GB".equals(normalized)
                || "EN_GB".equals(normalized)) {
            return "UK";
        }
        return "US";
    }

    /**
     * Resolves the default Edge-TTS neural voice name for a normalized accent.
     */
    public String resolveVoice(String normalizedAccent) {
        if (normalizedAccent != null && "UK".equalsIgnoreCase(normalizedAccent.trim())) {
            return VOICE_UK;
        }
        return VOICE_US;
    }

    /**
     * Resolves a canonical Edge-TTS neural voice from the accent's allowlist (case-insensitive),
     * falling back to {@link #resolveVoice(String)} when {@code requestedVoice} is blank or unsupported.
     */
    public String resolveVoice(String normalizedAccent, String requestedVoice) {
        String accentKey = normalizeAccent(normalizedAccent);
        return matchAllowedVoiceForAccent(accentKey, requestedVoice)
                .orElseGet(() -> resolveVoice(accentKey));
    }

    /**
     * Returns the canonical voice name if {@code candidateVoice} belongs to the allowlist for the given accent.
     */
    public static Optional<String> matchAllowedVoiceForAccent(String accent, String candidateVoice) {
        if (candidateVoice == null || candidateVoice.isBlank()) {
            return Optional.empty();
        }
        String trimmed = candidateVoice.trim();
        List<String> allowlist = (accent != null && "UK".equalsIgnoreCase(accent.trim()))
                ? ALLOWED_UK_VOICES
                : ALLOWED_US_VOICES;
        for (String allowed : allowlist) {
            if (allowed.equalsIgnoreCase(trimmed)) {
                return Optional.of(allowed);
            }
        }
        return Optional.empty();
    }

    /**
     * Sanitizes and validates input text for TTS synthesis.
     */
    public String cleanAndValidateText(String text) {
        String cleaned = sanitizeText(text);
        if (cleaned.isEmpty()) {
            throw new ParroTalkException(
                    "TTS text must not be blank.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST
            );
        }
        if (cleaned.length() > MAX_TEXT_LENGTH) {
            throw new ParroTalkException(
                    "TTS text exceeds maximum length of " + MAX_TEXT_LENGTH + " characters.",
                    ErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST
            );
        }
        return cleaned;
    }

    /**
     * Builds the relative TTS API URL for a given text and accent, or returns null if invalid.
     */
    public String buildTtsEndpointUrl(String text, String accent) {
        String cleaned = sanitizeText(text);
        if (cleaned.isEmpty() || cleaned.length() > MAX_TEXT_LENGTH) {
            return null;
        }
        return "/api/writing/tts?text="
                + URLEncoder.encode(cleaned, StandardCharsets.UTF_8)
                + "&accent="
                + normalizeAccent(accent);
    }

    /**
     * Computes a deterministic cache filename for cleaned text and normalized accent using the default accent voice.
     * Preserves text case sensitivity so acronyms (e.g. "US" vs "us") keep distinct audio.
     */
    public String computeCacheFileName(String cleanedText, String normalizedAccent) {
        String accentKey = normalizeAccent(normalizedAccent);
        String hashInput = accentKey + ":" + cleanedText;
        return accentKey.toLowerCase(Locale.ROOT) + "_" + sha256Hex(hashInput) + ".mp3";
    }

    /**
     * Computes a deterministic cache filename for cleaned text, normalized accent, and resolved voice.
     * Delegates to {@link #computeCacheFileName(String, String)} for the accent's default voice so
     * existing disk caches remain shared.
     */
    public String computeCacheFileName(String cleanedText, String normalizedAccent, String resolvedVoice) {
        String accentKey = normalizeAccent(normalizedAccent);
        String canonicalVoice = resolveVoice(accentKey, resolvedVoice);
        if (canonicalVoice.equals(resolveVoice(accentKey))) {
            return computeCacheFileName(cleanedText, accentKey);
        }
        String hashInput = accentKey + ":" + canonicalVoice + ":" + cleanedText;
        return accentKey.toLowerCase(Locale.ROOT) + "_" + sha256Hex(hashInput) + ".mp3";
    }

    /**
     * Resolves an already-cached, non-empty MP3 file path for the given text, accent, and optional voice
     * without triggering synthesis or reading the full file bytes into memory.
     */
    public Optional<Path> resolveCachedFileIfExists(String text, String accent, String requestedVoice) {
        try {
            String cleanedText = cleanAndValidateText(text);
            String normalizedAccent = normalizeAccent(accent);
            String voice = resolveVoice(normalizedAccent, requestedVoice);
            String cacheFileName = computeCacheFileName(cleanedText, normalizedAccent, voice);
            Path targetFile = Paths.get(ttsCacheDir, cacheFileName);
            if (Files.isRegularFile(targetFile) && Files.size(targetFile) > 0) {
                return Optional.of(targetFile);
            }
        } catch (Exception e) {
            log.debug("Unable to resolve cached TTS file: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Computes the deterministic cache key (without {@code .mp3} extension) matching Python {@code app/tts.py}.
     */
    public String computeCacheKey(String cleanedText, String normalizedAccent, String requestedVoice) {
        String fileName = computeCacheFileName(cleanedText, normalizedAccent, requestedVoice);
        return fileName.endsWith(".mp3") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    /**
     * Resolves an already-uploaded Cloudinary CDN URL from {@code tts_audio_cache} if available.
     */
    public Optional<String> resolveCloudinaryUrlIfCached(String text, String accent, String requestedVoice) {
        if (ttsAudioCacheRepository == null) {
            return Optional.empty();
        }
        try {
            String cleanedText = cleanAndValidateText(text);
            String normalizedAccent = normalizeAccent(accent);
            String voice = resolveVoice(normalizedAccent, requestedVoice);
            String cacheKey = computeCacheKey(cleanedText, normalizedAccent, voice);
            return ttsAudioCacheRepository.findById(cacheKey)
                    .map(TtsAudioCache::getCloudinaryUrl)
                    .filter(url -> url != null && !url.isBlank());
        } catch (ParroTalkException validationEx) {
            throw validationEx;
        } catch (Exception e) {
            log.debug("Unable to resolve Cloudinary TTS URL: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns MP3 pronunciation bytes for the given text and accent, serving from disk cache
     * or coordinating a single-flight Edge-TTS subprocess invocation.
     */
    public byte[] synthesize(String text, String accent) {
        return synthesizeInternal(text, accent, null);
    }

    /**
     * Returns MP3 pronunciation bytes for the given text, accent, and optional allowlisted Edge-TTS voice.
     */
    public byte[] synthesize(String text, String accent, String voice) {
        return synthesizeInternal(text, accent, voice);
    }

    private byte[] synthesizeInternal(String text, String accent, String requestedVoice) {
        String cleanedText = cleanAndValidateText(text);
        String normalizedAccent = normalizeAccent(accent);
        String voice = resolveVoice(normalizedAccent, requestedVoice);
        String cacheFileName = computeCacheFileName(cleanedText, normalizedAccent, voice);
        Path targetFile = Paths.get(ttsCacheDir, cacheFileName);

        byte[] cached = readCachedBytesIfValid(targetFile);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<byte[]> newFuture = new CompletableFuture<>();
        CompletableFuture<byte[]> existingFuture = inFlightRequests.putIfAbsent(cacheFileName, newFuture);
        if (existingFuture != null) {
            return awaitFuture(existingFuture);
        }

        try {
            byte[] doubleChecked = readCachedBytesIfValid(targetFile);
            if (doubleChecked != null) {
                newFuture.complete(doubleChecked);
                return doubleChecked;
            }

            byte[] synthesized = synthesizeWithPermit(cleanedText, voice, targetFile);
            newFuture.complete(synthesized);
            return synthesized;
        } catch (Throwable t) {
            newFuture.completeExceptionally(t);
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t instanceof Error err) {
                throw err;
            }
            throw new ParroTalkException(
                    "Failed to synthesize pronunciation audio.",
                    "TTS_SYNTHESIS_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } finally {
            inFlightRequests.remove(cacheFileName, newFuture);
        }
    }

    /**
     * Invokes Edge-TTS via subprocess, writes to a unique temporary file, and atomically moves
     * the completed MP3 to {@code targetFile}. Protected for unit testability.
     */
    protected byte[] runEdgeTtsProcess(String cleanedText, String voice, Path targetFile) {
        Path parentDir = targetFile.toAbsolutePath().getParent();
        Path tempFile = null;
        try {
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            tempFile = Files.createTempFile(parentDir, "tts_", ".tmp");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonCommand,
                    "-m", "edge_tts",
                    "--voice=" + voice,
                    "--text=" + cleanedText,
                    "--write-media=" + tempFile.toAbsolutePath().toString()
            );
            processBuilder.environment().put("PYTHONUTF8", "1");
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            Thread outputReader = new Thread(() -> {
                try (InputStream in = process.getInputStream()) {
                    in.transferTo(outputBuffer);
                } catch (IOException ignored) {
                }
            }, "edge-tts-output-reader");
            outputReader.setDaemon(true);
            outputReader.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(1000);
                throw new ParroTalkException(
                        "TTS synthesis timed out after " + timeoutSeconds + " seconds.",
                        "TTS_TIMEOUT",
                        HttpStatus.GATEWAY_TIMEOUT
                );
            }

            outputReader.join(1000);
            int exitCode = process.exitValue();
            String processOutput = outputBuffer.toString(StandardCharsets.UTF_8).trim();
            if (exitCode != 0) {
                log.error("Edge-TTS failed with exitCode={}: {}", exitCode, processOutput);
                throw new ParroTalkException(
                        "Failed to synthesize pronunciation audio.",
                        "TTS_SYNTHESIS_FAILED",
                        HttpStatus.BAD_GATEWAY
                );
            }

            if (!Files.exists(tempFile) || Files.size(tempFile) <= 0) {
                log.error("Edge-TTS generated empty audio file for voice={}, output={}", voice, processOutput);
                throw new ParroTalkException(
                        "Edge-TTS produced empty audio output.",
                        "TTS_EMPTY_AUDIO",
                        HttpStatus.BAD_GATEWAY
                );
            }

            byte[] audioBytes = Files.readAllBytes(tempFile);
            moveAtomicallyWithFallback(tempFile, targetFile);
            return audioBytes;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParroTalkException(
                    "TTS synthesis process was interrupted.",
                    "TTS_SYNTHESIS_INTERRUPTED",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (IOException e) {
            log.error("Edge-TTS I/O error: {}", e.getMessage(), e);
            throw new ParroTalkException(
                    "Failed to run TTS synthesis process.",
                    "TTS_SYNTHESIS_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.debug("Failed to delete temporary TTS file {}: {}", tempFile, e.getMessage());
                }
            }
        }
    }

    int availableProcessPermits() {
        return processSemaphore.availablePermits();
    }

    private byte[] synthesizeWithPermit(String cleanedText, String voice, Path targetFile) {
        boolean acquired = false;
        try {
            acquired = processSemaphore.tryAcquire(Math.max(1L, timeoutSeconds), TimeUnit.SECONDS);
            if (!acquired) {
                throw new ParroTalkException(
                        "TTS synthesis capacity is currently busy. Please try again shortly.",
                        "TTS_SERVICE_BUSY",
                        HttpStatus.SERVICE_UNAVAILABLE
                );
            }
            return runEdgeTtsProcess(cleanedText, voice, targetFile);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParroTalkException(
                    "TTS synthesis was interrupted while waiting for process permit.",
                    "TTS_SYNTHESIS_INTERRUPTED",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } finally {
            if (acquired) {
                processSemaphore.release();
            }
        }
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }
        return CONTROL_CHARS_PATTERN.matcher(text)
                .replaceAll("")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private byte[] readCachedBytesIfValid(Path targetFile) {
        try {
            if (Files.exists(targetFile) && Files.size(targetFile) > 0) {
                return Files.readAllBytes(targetFile);
            }
        } catch (IOException e) {
            log.warn("Failed to read cached TTS audio {}: {}", targetFile, e.getMessage());
        }
        return null;
    }

    private byte[] awaitFuture(CompletableFuture<byte[]> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParroTalkException(
                    "TTS synthesis was interrupted while waiting for in-flight request.",
                    "TTS_SYNTHESIS_INTERRUPTED",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ParroTalkException pte) {
                throw pte;
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new ParroTalkException(
                    "Failed to synthesize pronunciation audio.",
                    "TTS_SYNTHESIS_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void moveAtomicallyWithFallback(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}

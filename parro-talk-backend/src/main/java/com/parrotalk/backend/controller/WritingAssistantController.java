package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.writing.WritingAnalyzeRequestDto;
import com.parrotalk.backend.dto.writing.WritingDraftUpdateRequestDto;
import com.parrotalk.backend.dto.writing.WritingSessionResponseDto;
import com.parrotalk.backend.dto.writing.WritingSessionSummaryDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.TtsPrewarmService;
import com.parrotalk.backend.service.WritingAssistantService;
import com.parrotalk.backend.service.WritingTtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for the AI Writing Assistant feature.
 */
@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingAssistantController {

    private final WritingAssistantService writingService;
    private final WritingTtsService writingTtsService;
    private TtsPrewarmService ttsPrewarmService;
    private boolean localFallbackEnabled = false;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setTtsPrewarmService(TtsPrewarmService ttsPrewarmService) {
        this.ttsPrewarmService = ttsPrewarmService;
    }

    @org.springframework.beans.factory.annotation.Value("${app.tts.local-fallback-enabled:false}")
    void setLocalFallbackEnabled(boolean localFallbackEnabled) {
        this.localFallbackEnabled = localFallbackEnabled;
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u && u.getId() != null) {
            return u;
        }
        return null;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<WritingSessionResponseDto>> analyze(
            @Valid @RequestBody WritingAnalyzeRequestDto request) {
        User user = getCurrentUser();
        WritingSessionResponseDto response = writingService.analyzeAndSave(request, user);
        return ResponseEntity.ok(ApiResponse.<WritingSessionResponseDto>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<PageResponse<WritingSessionSummaryDto>>> getSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = getCurrentUser();
        PageResponse<WritingSessionSummaryDto> response = writingService.getUserSessions(user, page, size);
        return ResponseEntity.ok(ApiResponse.<PageResponse<WritingSessionSummaryDto>>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<WritingSessionResponseDto>> getSessionDetail(
            @PathVariable UUID sessionId) {
        User user = getCurrentUser();
        WritingSessionResponseDto response = writingService.getSessionDetail(sessionId, user);
        return ResponseEntity.ok(ApiResponse.<WritingSessionResponseDto>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @PatchMapping("/sessions/{sessionId}/draft")
    public ResponseEntity<ApiResponse<WritingSessionResponseDto>> updateDraft(
            @PathVariable UUID sessionId,
            @Valid @RequestBody WritingDraftUpdateRequestDto request) {
        User user = getCurrentUser();
        WritingSessionResponseDto response = writingService.updateUserDraft(sessionId, request.getUserDraft(), user);
        return ResponseEntity.ok(ApiResponse.<WritingSessionResponseDto>builder()
                .code(1000)
                .result(response)
                .build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable UUID sessionId) {
        User user = getCurrentUser();
        writingService.deleteSession(sessionId, user);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Deleted writing session successfully.")
                .build());
    }

    private static final MediaType AUDIO_MPEG_MEDIA_TYPE = MediaType.parseMediaType("audio/mpeg");
    private static final String TTS_CACHE_CONTROL = "public, max-age=604800, immutable";

    /**
     * Synthesizes or streams cached US/UK pronunciation audio via Edge-TTS, supporting
     * {@code ETag}/{@code If-None-Match} (304) and HTTP {@code Range} partial content (206).
     */
    @GetMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<Resource> synthesizeTts(
            @RequestParam("text") String text,
            @RequestParam(value = "accent", defaultValue = "US") String accent,
            @RequestParam(value = "voice", required = false) String voice,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws IOException {
        String normalizedVoice = (voice == null || voice.isBlank()) ? null : voice;

        Optional<String> cloudinaryUrl = writingTtsService.resolveCloudinaryUrlIfCached(text, accent, normalizedVoice);
        if (cloudinaryUrl.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(cloudinaryUrl.get()))
                    .header(HttpHeaders.CACHE_CONTROL, TTS_CACHE_CONTROL)
                    .build();
        }

        Optional<Path> cachedPath = writingTtsService.resolveCachedFileIfExists(text, accent, normalizedVoice);
        Path resolvedFilePath = null;
        Resource resource;
        String eTag;
        if (cachedPath.isPresent()) {
            resolvedFilePath = cachedPath.get();
            resource = new FileSystemResource(resolvedFilePath);
            eTag = computeFileETag(resolvedFilePath);
        } else if (!localFallbackEnabled) {
            if (ttsPrewarmService != null) {
                ttsPrewarmService.enqueueSingleSynthesis(text, accent, normalizedVoice);
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else {
            byte[] audioBytes = normalizedVoice == null
                    ? writingTtsService.synthesize(text, accent)
                    : writingTtsService.synthesize(text, accent, normalizedVoice);

            Optional<Path> newlyCachedPath = writingTtsService.resolveCachedFileIfExists(text, accent, normalizedVoice);
            if (newlyCachedPath.isPresent()) {
                resolvedFilePath = newlyCachedPath.get();
                resource = new FileSystemResource(resolvedFilePath);
                eTag = computeFileETag(resolvedFilePath);
            } else {
                resource = new ByteArrayResource(audioBytes);
                eTag = computeBytesETag(audioBytes);
            }
        }

        if (matchesIfNoneMatch(ifNoneMatch, eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(eTag)
                    .header(HttpHeaders.CACHE_CONTROL, TTS_CACHE_CONTROL)
                    .build();
        }

        long contentLength = resource.contentLength();
        if (rangeHeader != null && !rangeHeader.isBlank()) {
            List<HttpRange> ranges;
            try {
                ranges = HttpRange.parseRanges(rangeHeader);
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .build();
            }
            if (!ranges.isEmpty()) {
                HttpRange range = ranges.get(0);
                long start = range.getRangeStart(contentLength);
                long end = range.getRangeEnd(contentLength);
                if (contentLength <= 0 || start < 0 || start >= contentLength || end < start) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .build();
                }
                long rangeLength = end - start + 1;
                ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
                Resource partialResource = createPartialResource(region, resolvedFilePath);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(AUDIO_MPEG_MEDIA_TYPE)
                        .contentLength(rangeLength)
                        .eTag(eTag)
                        .header(HttpHeaders.CACHE_CONTROL, TTS_CACHE_CONTROL)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
                        .body(partialResource);
            }
        }

        return ResponseEntity.ok()
                .contentType(AUDIO_MPEG_MEDIA_TYPE)
                .contentLength(contentLength)
                .eTag(eTag)
                .header(HttpHeaders.CACHE_CONTROL, TTS_CACHE_CONTROL)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    private Resource createPartialResource(ResourceRegion region, Path cachedPath) {
        long start = region.getPosition();
        long length = region.getCount();
        return new AbstractResource() {
            @Override
            public String getDescription() {
                return "Partial audio resource [" + start + "-" + (start + length - 1) + "]";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                if (cachedPath != null) {
                    SeekableByteChannel channel = Files.newByteChannel(cachedPath, StandardOpenOption.READ);
                    try {
                        channel.position(start);
                        return boundedInputStream(Channels.newInputStream(channel), length);
                    } catch (IOException | RuntimeException ex) {
                        channel.close();
                        throw ex;
                    }
                }
                InputStream delegate = region.getResource().getInputStream();
                try {
                    delegate.skipNBytes(start);
                    return boundedInputStream(delegate, length);
                } catch (IOException | RuntimeException ex) {
                    delegate.close();
                    throw ex;
                }
            }

            @Override
            public long contentLength() {
                return length;
            }
        };
    }

    private InputStream boundedInputStream(InputStream delegate, long limit) {
        return new FilterInputStream(delegate) {
            private long remaining = limit;

            @Override
            public int read() throws IOException {
                if (remaining <= 0) {
                    return -1;
                }
                int b = super.read();
                if (b >= 0) {
                    remaining--;
                }
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (remaining <= 0) {
                    return -1;
                }
                int toRead = (int) Math.min(len, remaining);
                int readBytes = super.read(b, off, toRead);
                if (readBytes > 0) {
                    remaining -= readBytes;
                }
                return readBytes;
            }
        };
    }

    private String computeFileETag(Path path) throws IOException {
        String filename = path.getFileName().toString();
        String baseName = filename.endsWith(".mp3")
                ? filename.substring(0, filename.length() - 4)
                : filename;
        return "\"" + baseName + "-" + Files.size(path) + "\"";
    }

    private String computeBytesETag(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return "\"" + HexFormat.of().formatHex(digest).substring(0, 32) + "-" + bytes.length + "\"";
        } catch (NoSuchAlgorithmException e) {
            return "\"" + bytes.length + "\"";
        }
    }

    private boolean matchesIfNoneMatch(String ifNoneMatchHeader, String currentETag) {
        if (ifNoneMatchHeader == null || ifNoneMatchHeader.isBlank() || currentETag == null) {
            return false;
        }
        String normalizedCurrent = stripWeakPrefix(currentETag.trim());
        for (String candidate : ifNoneMatchHeader.split(",")) {
            String trimmed = candidate.trim();
            if ("*".equals(trimmed)) {
                return true;
            }
            if (stripWeakPrefix(trimmed).equals(normalizedCurrent)) {
                return true;
            }
        }
        return false;
    }

    private String stripWeakPrefix(String tag) {
        return tag.startsWith("W/") ? tag.substring(2).trim() : tag;
    }
}


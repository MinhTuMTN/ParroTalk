package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserNotification;
import com.parrotalk.backend.service.SseService;
import com.parrotalk.backend.service.UserNotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService notificationService;
    private final SseService sseService;

    public record NotificationDto(
            UUID id,
            String type,
            String title,
            String message,
            UUID referenceId,
            String payloadJson,
            boolean isRead,
            LocalDateTime createdAt
    ) {}

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(@AuthenticationPrincipal User user) {
        List<NotificationDto> items = notificationService.getRecentNotifications(user.getId()).stream()
                .map(this::toDto)
                .toList();
        long unreadCount = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .result(Map.of(
                        "items", items,
                        "unreadCount", unreadCount
                ))
                .build());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        long unreadCount = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .result(unreadCount)
                .build());
    }

    @RequestMapping(value = "/{notificationId}/read", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable UUID notificationId) {
        notificationService.markAsRead(user.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        return sseService.connect(user.getId());
    }

    private NotificationDto toDto(UserNotification n) {
        return new NotificationDto(
                n.getId(),
                n.getType() != null ? n.getType().name() : "",
                n.getTitle(),
                n.getMessage(),
                n.getReferenceId(),
                n.getPayloadJson(),
                Boolean.TRUE.equals(n.getIsRead()),
                n.getCreatedAt()
        );
    }
}

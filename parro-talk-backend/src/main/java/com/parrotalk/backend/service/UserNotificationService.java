package com.parrotalk.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.constant.UserNotificationType;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.UserNotification;
import com.parrotalk.backend.repository.UserNotificationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository notificationRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserNotification notifyUser(
            User user,
            UserNotificationType type,
            String title,
            String message,
            UUID referenceId,
            Map<String, Object> payload) {
        String payloadJson = null;
        if (payload != null && !payload.isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize notification payload: {}", e.getMessage());
            }
        }

        UserNotification notification = UserNotification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .payloadJson(payloadJson)
                .isRead(false)
                .build();

        UserNotification saved = notificationRepository.save(notification);

        if (user != null && user.getId() != null) {
            sseService.sendEvent(user.getId(), Map.of(
                    "notificationId", saved.getId() != null ? saved.getId().toString() : "",
                    "type", type.name(),
                    "title", title,
                    "message", message,
                    "referenceId", referenceId != null ? referenceId.toString() : "",
                    "payload", payload != null ? payload : Map.of()
            ));
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<UserNotification> getRecentNotifications(UUID userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser() != null && userId.equals(n.getUser().getId())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<UserNotification> list = notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        for (UserNotification n : list) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }
}


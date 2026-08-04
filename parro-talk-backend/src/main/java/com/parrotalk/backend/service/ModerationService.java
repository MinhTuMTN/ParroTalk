package com.parrotalk.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.ModerationField;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.constant.ModerationTargetType;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.moderation.ModerationEventResponse;
import com.parrotalk.backend.dto.moderation.ModerationUpdateRequest;
import com.parrotalk.backend.entity.ModerationEvent;
import com.parrotalk.backend.entity.ModerationState;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.ModerationEventRepository;
import com.parrotalk.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Triage workflow shared by every moderated domain.
 *
 * <p>
 * Holds the state-transition rules and the audit trail so that
 * {@link LessonReportService} and {@link AppFeedbackService} contain only
 * their own domain logic. Adding a third moderated domain requires no change
 * here.
 * </p>
 *
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {

    /** Statuses that still belong to the working queue. */
    public static final List<ModerationStatus> ACTIVE_STATUSES = List.of(
            ModerationStatus.OPEN, ModerationStatus.IN_REVIEW);

    private final ModerationEventRepository moderationEventRepository;
    private final UserRepository userRepository;

    /**
     * Record the creation of a moderated item.
     *
     * @param targetType Domain of the item
     * @param targetId   Item id
     * @param actor      User who submitted it
     */
    @Transactional
    public void recordCreation(ModerationTargetType targetType, UUID targetId, User actor) {
        moderationEventRepository.save(ModerationEvent.builder()
                .targetType(targetType)
                .targetId(targetId)
                .actor(actor)
                .field(ModerationField.CREATED)
                .newValue(ModerationStatus.OPEN.name())
                .build());
    }

    /**
     * Apply a partial triage update and append one audit entry per changed
     * field.
     *
     * @param state      Triage state of the item, mutated in place
     * @param targetType Domain of the item
     * @param targetId   Item id
     * @param request    Requested changes
     * @param actor      Admin performing the change
     */
    @Transactional
    public void applyUpdate(
            ModerationState state,
            ModerationTargetType targetType,
            UUID targetId,
            ModerationUpdateRequest request,
            User actor) {

        List<ModerationEvent> events = new ArrayList<>();

        applyStatus(state, request, events);
        applyPriority(state, request, events);
        applyAssignee(state, request, events);
        applyResolutionNote(state, request, events);

        if (events.isEmpty()) {
            throw new ParroTalkException(
                    "No moderation field was changed.",
                    "MODERATION_NO_CHANGES",
                    HttpStatus.BAD_REQUEST);
        }

        events.forEach(event -> {
            event.setTargetType(targetType);
            event.setTargetId(targetId);
            event.setActor(actor);
            event.setNote(request.note());
        });
        moderationEventRepository.saveAll(events);

        log.info("Moderation update applied: targetType={}, targetId={}, actorId={}, fields={}",
                targetType, targetId, actor.getId(), events.stream().map(ModerationEvent::getField).toList());
    }

    /**
     * Read the audit trail of a moderated item.
     *
     * @param targetType Domain of the item
     * @param targetId   Item id
     * @return Ordered audit entries
     */
    @Transactional(readOnly = true)
    public List<ModerationEventResponse> getEvents(ModerationTargetType targetType, UUID targetId) {
        return moderationEventRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId)
                .stream()
                .map(ModerationEventResponse::from)
                .toList();
    }

    private void applyStatus(ModerationState state, ModerationUpdateRequest request, List<ModerationEvent> events) {
        ModerationStatus newStatus = request.status();
        if (newStatus == null || newStatus == state.getStatus()) {
            return;
        }

        events.add(change(ModerationField.STATUS, state.getStatus().name(), newStatus.name()));
        state.setStatus(newStatus);
        state.setResolvedAt(newStatus.isTerminal() ? LocalDateTime.now() : null);
    }

    private void applyPriority(ModerationState state, ModerationUpdateRequest request, List<ModerationEvent> events) {
        if (request.priority() == null || request.priority() == state.getPriority()) {
            return;
        }

        events.add(change(ModerationField.PRIORITY, state.getPriority().name(), request.priority().name()));
        state.setPriority(request.priority());
    }

    private void applyAssignee(ModerationState state, ModerationUpdateRequest request, List<ModerationEvent> events) {
        if (request.unassign() && request.assigneeId() != null) {
            throw new ParroTalkException(
                    "Cannot specify assigneeId when unassign flag is true.",
                    "MODERATION_INVALID_ASSIGNMENT",
                    HttpStatus.BAD_REQUEST);
        }

        UUID currentAssigneeId = state.getAssigneeId();

        if (request.unassign()) {
            if (currentAssigneeId == null) {
                return;
            }
            events.add(change(ModerationField.ASSIGNEE, currentAssigneeId.toString(), null));
            state.setAssignee(null);
            return;
        }

        if (request.assigneeId() == null || Objects.equals(request.assigneeId(), currentAssigneeId)) {
            return;
        }

        User assignee = requireAdmin(request.assigneeId());
        events.add(change(
                ModerationField.ASSIGNEE,
                currentAssigneeId == null ? null : currentAssigneeId.toString(),
                assignee.getId().toString()));
        state.setAssignee(assignee);
    }

    private void applyResolutionNote(
            ModerationState state, ModerationUpdateRequest request, List<ModerationEvent> events) {
        if (request.resolutionNote() == null || Objects.equals(request.resolutionNote(), state.getResolutionNote())) {
            return;
        }

        // The note itself is user-visible content and is not copied into the
        // audit value columns to keep the log free of free-form text.
        events.add(change(ModerationField.RESOLUTION_NOTE, null, null));
        state.setResolutionNote(request.resolutionNote());
    }

    private User requireAdmin(UUID assigneeId) {
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ParroTalkException(
                        "Assignee not found.", "ASSIGNEE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (assignee.getRole() != Role.ADMIN) {
            throw new ParroTalkException(
                    "Only an administrator can be assigned.", "ASSIGNEE_NOT_ADMIN", HttpStatus.BAD_REQUEST);
        }
        return assignee;
    }

    private ModerationEvent change(ModerationField field, String oldValue, String newValue) {
        return ModerationEvent.builder()
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
    }
}

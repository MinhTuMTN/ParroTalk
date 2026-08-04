package com.parrotalk.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.FeedbackCategory;
import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.constant.ModerationTargetType;
import com.parrotalk.backend.util.PageableUtils;
import org.springframework.data.domain.Sort;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.feedback.AppFeedbackResponse;
import com.parrotalk.backend.dto.feedback.CreateAppFeedbackRequest;
import com.parrotalk.backend.dto.moderation.ModerationEventResponse;
import com.parrotalk.backend.dto.moderation.ModerationUpdateRequest;
import com.parrotalk.backend.entity.AppFeedback;
import com.parrotalk.backend.entity.ModerationState;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.AppFeedbackRepository;
import com.parrotalk.backend.specification.AppFeedbackSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feedback and bug reports about the application itself.
 *
 * <p>
 * Fully independent from {@link LessonReportService}; the only shared piece is
 * the generic triage workflow of {@link ModerationService}.
 * </p>
 *
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppFeedbackService {

    private static final ModerationTargetType TARGET_TYPE = ModerationTargetType.APP_FEEDBACK;

    private final AppFeedbackRepository appFeedbackRepository;
    private final ModerationService moderationService;

    /**
     * Submit a new feedback.
     *
     * @param user    Authenticated submitter
     * @param request Feedback payload
     * @return Created feedback
     */
    @Transactional
    public AppFeedbackResponse create(User user, CreateAppFeedbackRequest request) {
        AppFeedback feedback = appFeedbackRepository.save(AppFeedback.builder()
                .user(user)
                .category(request.category())
                .title(request.title().trim())
                .description(request.description().trim())
                .moderationState(ModerationState.initial())
                .build());

        moderationService.recordCreation(TARGET_TYPE, feedback.getId(), user);

        log.info("App feedback created: feedbackId={}, category={}, userId={}",
                feedback.getId(), feedback.getCategory(), user.getId());

        return AppFeedbackResponse.from(feedback);
    }

    /**
     * List the feedback the caller submitted.
     *
     * @param user Authenticated submitter
     * @param page Page index
     * @param size Page size
     * @return Page of own feedback
     */
    @Transactional(readOnly = true)
    public PageResponse<AppFeedbackResponse> listOwnFeedback(User user, int page, int size) {
        return toPageResponse(appFeedbackRepository
                .findByUserId(user.getId(), PageableUtils.createPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    /**
     * Admin listing with optional filters.
     *
     * @param status     Triage status filter
     * @param category   Category filter
     * @param priority   Priority filter
     * @param assigneeId Assignee filter
     * @param page       Page index
     * @param size       Page size
     * @return Page of feedback
     */
    @Transactional(readOnly = true)
    public PageResponse<AppFeedbackResponse> search(
            ModerationStatus status,
            FeedbackCategory category,
            ModerationPriority priority,
            UUID assigneeId,
            int page,
            int size) {

        Specification<AppFeedback> specification = Specification
                .where(AppFeedbackSpecification.hasStatus(status))
                .and(AppFeedbackSpecification.hasCategory(category))
                .and(AppFeedbackSpecification.hasPriority(priority))
                .and(AppFeedbackSpecification.hasAssignee(assigneeId));

        return toPageResponse(appFeedbackRepository.findAll(specification, PageableUtils.createPageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    /**
     * Read a single feedback.
     *
     * @param feedbackId Feedback id
     * @return Feedback detail
     */
    @Transactional(readOnly = true)
    public AppFeedbackResponse getDetail(UUID feedbackId) {
        return AppFeedbackResponse.from(findFeedback(feedbackId));
    }

    /**
     * Update the triage state of a feedback.
     *
     * @param feedbackId Feedback id
     * @param request    Requested changes
     * @param actor      Admin performing the change
     * @return Updated feedback
     */
    @Transactional
    public AppFeedbackResponse updateModeration(UUID feedbackId, ModerationUpdateRequest request, User actor) {
        AppFeedback feedback = findFeedback(feedbackId);
        moderationService.applyUpdate(feedback.getModerationState(), TARGET_TYPE, feedbackId, request, actor);
        return AppFeedbackResponse.from(appFeedbackRepository.save(feedback));
    }

    /**
     * Read the audit trail of a feedback.
     *
     * @param feedbackId Feedback id
     * @return Ordered audit entries
     */
    @Transactional(readOnly = true)
    public List<ModerationEventResponse> getEvents(UUID feedbackId) {
        findFeedback(feedbackId);
        return moderationService.getEvents(TARGET_TYPE, feedbackId);
    }

    private AppFeedback findFeedback(UUID feedbackId) {
        return appFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ParroTalkException(
                        "Feedback not found.", "APP_FEEDBACK_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private PageResponse<AppFeedbackResponse> toPageResponse(Page<AppFeedback> result) {
        return PageResponse.<AppFeedbackResponse>builder()
                .content(result.getContent().stream().map(AppFeedbackResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }
}

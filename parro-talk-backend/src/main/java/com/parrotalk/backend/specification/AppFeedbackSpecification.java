package com.parrotalk.backend.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.parrotalk.backend.constant.FeedbackCategory;
import com.parrotalk.backend.constant.ModerationPriority;
import com.parrotalk.backend.constant.ModerationStatus;
import com.parrotalk.backend.entity.AppFeedback;

/**
 * App Feedback Specification.
 *
 * @author MinhTuMTN
 */
public class AppFeedbackSpecification {

    private AppFeedbackSpecification() {
    }

    /**
     * Filter by triage status.
     *
     * @param status Status, may be {@code null}
     * @return Specification
     */
    public static Specification<AppFeedback> hasStatus(ModerationStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("moderationState").get("status"), status);
    }

    /**
     * Filter by category.
     *
     * @param category Category, may be {@code null}
     * @return Specification
     */
    public static Specification<AppFeedback> hasCategory(FeedbackCategory category) {
        return (root, query, cb) -> category == null
                ? cb.conjunction()
                : cb.equal(root.get("category"), category);
    }

    /**
     * Filter by triage priority.
     *
     * @param priority Priority, may be {@code null}
     * @return Specification
     */
    public static Specification<AppFeedback> hasPriority(ModerationPriority priority) {
        return (root, query, cb) -> priority == null
                ? cb.conjunction()
                : cb.equal(root.get("moderationState").get("priority"), priority);
    }

    /**
     * Filter by assigned admin.
     *
     * @param assigneeId Assignee id, may be {@code null}
     * @return Specification
     */
    public static Specification<AppFeedback> hasAssignee(UUID assigneeId) {
        return (root, query, cb) -> assigneeId == null
                ? cb.conjunction()
                : cb.equal(root.get("moderationState").get("assignee").get("id"), assigneeId);
    }
}

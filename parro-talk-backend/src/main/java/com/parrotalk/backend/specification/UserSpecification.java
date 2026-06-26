package com.parrotalk.backend.specification;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.UserStatus;
import com.parrotalk.backend.entity.User;

public class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> matchesSearch(String keyword) {
        return (root, query, cb) -> {
            if (StringUtils.isBlank(keyword)) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("displayUsername")), pattern));
        };
    }

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null
                ? cb.conjunction()
                : cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("enabled"), status == UserStatus.ACTIVE);
        };
    }
}

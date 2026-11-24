package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * User Repository.
 * 
 * @author MinhTuMTN
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByDisplayUsernameIgnoreCase(String username);

    /**
     * Find user by email (include deleted users).
     *
     * @param email Email address to search
     * @return Optional of User
     */
    @Query(
        value = "SELECT * FROM users WHERE LOWER(email) = LOWER(:email)",
        nativeQuery = true)
    Optional<User> findByEmailIncludeDeleted(@Param("email") String email);

    /**
     * Update user's last active timestamp without loading the full entity.
     *
     * @param userId User id
     * @param now    Activity timestamp
     * @return Number of updated rows
     */
    @Modifying
    @Query("UPDATE User u SET u.lastActiveAt = :now, u.updatedAt = :now WHERE u.id = :userId AND u.isDeleted = false")
    int updateLastActiveAt(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}

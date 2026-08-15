package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

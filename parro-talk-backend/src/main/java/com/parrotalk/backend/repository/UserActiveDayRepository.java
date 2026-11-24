package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserActiveDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserActiveDayRepository extends JpaRepository<UserActiveDay, Long> {
    Optional<UserActiveDay> findByUserIdAndActiveDate(UUID userId, LocalDate activeDate);

    /**
     * Check whether an active day record already exists for the user and date.
     *
     * @param userId     User id
     * @param activeDate Active date
     * @return True if recorded, false otherwise
     */
    boolean existsByUserIdAndActiveDate(UUID userId, LocalDate activeDate);

    /**
     * Insert an active day record if not already present in PostgreSQL.
     *
     * @param userId     User id
     * @param activeDate Active date
     * @return Number of inserted rows (0 or 1)
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_active_days (user_id, active_date)
            VALUES (:userId, :activeDate)
            ON CONFLICT (user_id, active_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("activeDate") LocalDate activeDate);

    List<UserActiveDay> findAllByUserIdOrderByActiveDateDesc(UUID userId);
    List<UserActiveDay> findAllByUserIdOrderByActiveDateAsc(UUID userId);
    List<UserActiveDay> findAllByUserIdAndActiveDateBetweenOrderByActiveDateAsc(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate);
}

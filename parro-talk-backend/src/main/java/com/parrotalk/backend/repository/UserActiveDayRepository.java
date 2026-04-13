package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.UserActiveDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserActiveDayRepository extends JpaRepository<UserActiveDay, Long> {
    Optional<UserActiveDay> findByUserIdAndActiveDate(UUID userId, LocalDate activeDate);
    List<UserActiveDay> findAllByUserIdOrderByActiveDateDesc(UUID userId);
}

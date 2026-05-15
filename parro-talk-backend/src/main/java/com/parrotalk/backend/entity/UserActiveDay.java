package com.parrotalk.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * User active day entity.
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "user_active_days", indexes = {
        @Index(name = "idx_user_active_days_user_date", columnList = "user_id, active_date"),
        @Index(name = "idx_user_active_days_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_active_days_user_date", columnNames = { "user_id", "active_date" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActiveDay {

    /** User active day ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User ID */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Active date */
    @Column(name = "active_date", nullable = false)
    private LocalDate activeDate;
}

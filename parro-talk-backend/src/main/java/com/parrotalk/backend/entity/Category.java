package com.parrotalk.backend.entity;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;
import java.util.UUID;

/**
 * Lesson category.
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE categories SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Category extends BaseEntity {

    /** Category ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Category Name */
    @Column(nullable = false, unique = true)
    private String name;

    /** Thumnail url */
    @Column(nullable = true)
    private String thumbnailUrl;

    /** Lessons belong to this category */
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private List<Lesson> lessons;
}

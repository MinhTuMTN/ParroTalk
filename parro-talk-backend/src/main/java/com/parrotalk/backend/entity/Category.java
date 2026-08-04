package com.parrotalk.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import com.parrotalk.backend.constant.CmsItemStatus;

import java.util.List;
import java.util.UUID;

/**
 * Lesson category.
 * 
 * @author MinhTuMTN
 */
@Entity
@Table(name = "lesson_categories", indexes = {
    @Index(name = "idx_lesson_categories_slug", columnList = "slug"),
    @Index(name = "idx_lesson_categories_parent_id", columnList = "parent_category_id"),
    @Index(name = "idx_lesson_categories_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE lesson_categories SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Category extends BaseEntity {

    /** Category ID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Category Name */
    @Column(nullable = false)
    private String name;

    /** Slug (unique) */
    @Column(nullable = false, unique = true)
    private String slug;

    /** Description */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Icon */
    @Column
    private String icon;

    /** Color */
    @Column
    private String color;

    /** Image URL */
    @Column(name = "image_url")
    private String imageUrl;

    /** Parent Category ID */
    @Column(name = "parent_category_id")
    private UUID parentCategoryId;

    /** Materialized Path for Tree Structure (e.g. /uuid1/uuid2/) */
    @Column
    private String path;

    /** Sort Order */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CmsItemStatus status = CmsItemStatus.ACTIVE;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    /** Lessons belong to this category */
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private List<Lesson> lessons;
}

package com.parrotalk.backend.entity;

import com.parrotalk.backend.constant.CategoryStatus;
import jakarta.persistence.*;
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
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_slug", columnList = "slug"),
        @Index(name = "idx_categories_status", columnList = "status"),
        @Index(name = "idx_categories_display_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE categories SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Category extends BaseEntity {

    /** Category ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Category Name. */
    @Column(nullable = false, unique = true)
    private String name;

    /** SEO/user-friendly slug. */
    @Column(nullable = false, unique = true)
    private String slug;

    /** Category description. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Icon name from the frontend icon set. */
    @Column(name = "icon_name")
    private String iconName;

    /** Icon URL for custom category icon. */
    @Column(name = "icon_url")
    private String iconUrl;

    /** Thumbnail url kept for backward compatibility. */
    @Column(nullable = true)
    private String thumbnailUrl;

    /** Category display color. */
    @Column
    private String color;

    /** Category status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CategoryStatus status = CategoryStatus.ACTIVE;

    /** Category display order. */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /** Lessons belong to this category. */
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    private List<Lesson> lessons;
}

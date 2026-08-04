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

@Entity
@Table(name = "lesson_tags", indexes = {
    @Index(name = "idx_lesson_tags_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE lesson_tags SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 50)
    private String color;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CmsItemStatus status = CmsItemStatus.ACTIVE;
    
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private List<Lesson> lessons;
}

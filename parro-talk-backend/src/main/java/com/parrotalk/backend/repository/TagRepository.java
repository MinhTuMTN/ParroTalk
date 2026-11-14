package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID>, JpaSpecificationExecutor<Tag> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Tag> findBySlug(String slug);

    @Query("SELECT COUNT(l) FROM Tag t JOIN t.lessons l WHERE t.id = :tagId")
    long countLessonsByTagId(@Param("tagId") UUID tagId);

    @Query("SELECT t.id, COUNT(l) FROM Tag t JOIN t.lessons l WHERE t.id IN :tagIds GROUP BY t.id")
    List<Object[]> countLessonsByTagIds(@Param("tagIds") Collection<UUID> tagIds);

    Optional<Tag> findByName(String name);

    List<Tag> findByNameIn(Collection<String> names);
}

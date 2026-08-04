package com.parrotalk.backend.repository;

import com.parrotalk.backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Tag> findBySlug(String slug);

    @Query("SELECT COUNT(l) FROM Tag t JOIN t.lessons l WHERE t.id = :tagId")
    long countLessonsByTagId(@Param("tagId") UUID tagId);

    Optional<Tag> findByName(String name);

    List<Tag> findByNameIn(Collection<String> names);
}

package com.parrotalk.backend.service.cms;

import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.admin.AdminTagCreateRequest;
import com.parrotalk.backend.dto.admin.AdminTagDto;
import com.parrotalk.backend.dto.admin.AdminTagUpdateRequest;
import com.parrotalk.backend.entity.Tag;
import com.parrotalk.backend.entity.cms.AuditLog;
import com.parrotalk.backend.mapper.admin.AdminTagMapper;
import com.parrotalk.backend.repository.TagRepository;
import com.parrotalk.backend.repository.cms.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final TagRepository tagRepository;
    private final AdminTagMapper tagMapper;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminTagDto> searchTags(String search, CmsItemStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());

        Specification<Tag> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("slug")), pattern)
            ));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Tag> tagPage = tagRepository.findAll(spec, pageable);

        List<Tag> tags = tagPage.getContent();
        Map<UUID, Long> countMap = new HashMap<>();
        if (!tags.isEmpty()) {
            List<UUID> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toList());
            List<Object[]> counts = tagRepository.countLessonsByTagIds(tagIds);
            for (Object[] row : counts) {
                if (row.length >= 2 && row[0] instanceof UUID && row[1] instanceof Number) {
                    countMap.put((UUID) row[0], ((Number) row[1]).longValue());
                }
            }
        }

        List<AdminTagDto> content = tags.stream().map(t -> {
            AdminTagDto dto = tagMapper.toDto(t);
            dto.setLessonsCount(countMap.getOrDefault(t.getId(), 0L));
            return dto;
        }).collect(Collectors.toList());

        return PageResponse.<AdminTagDto>builder()
                .content(content)
                .page(tagPage.getNumber())
                .size(tagPage.getSize())
                .totalPages(tagPage.getTotalPages())
                .totalElements(tagPage.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminTagDto> searchTags(int page, int size) {
        return searchTags(null, null, page, size);
    }

    @Transactional(readOnly = true)
    public AdminTagDto getTag(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        AdminTagDto dto = tagMapper.toDto(tag);
        dto.setLessonsCount(tagRepository.countLessonsByTagId(tag.getId()));
        return dto;
    }

    @Transactional
    public AdminTagDto createTag(AdminTagCreateRequest request) {
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getName());
        } else {
            slug = generateSlug(slug);
        }
        
        if (tagRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug already exists");
        }

        Tag tag = Tag.builder()
                .name(request.getName())
                .slug(slug)
                .color(request.getColor())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();

        tag = tagRepository.save(tag);
        logAudit("Tag", tag.getId(), "CREATE");

        return tagMapper.toDto(tag);
    }

    private String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    @Transactional
    public AdminTagDto updateTag(UUID id, AdminTagUpdateRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        if (tagRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new IllegalArgumentException("Slug already exists");
        }

        tag.setName(request.getName());
        tag.setSlug(request.getSlug());
        tag.setColor(request.getColor());
        tag.setDescription(request.getDescription());
        tag.setStatus(request.getStatus());

        tagRepository.save(tag);
        logAudit("Tag", tag.getId(), "UPDATE");

        return tagMapper.toDto(tag);
    }

    @Transactional
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        long lessonsCount = tagRepository.countLessonsByTagId(id);
        if (lessonsCount > 0) {
            throw new IllegalStateException("Cannot delete tag used by " + lessonsCount + " lessons");
        }

        tagRepository.delete(tag);
        logAudit("Tag", id, "DELETE");
    }

    private void logAudit(String entityName, UUID entityId, String action) {
        AuditLog log = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .build();
        auditLogRepository.save(log);
    }
}

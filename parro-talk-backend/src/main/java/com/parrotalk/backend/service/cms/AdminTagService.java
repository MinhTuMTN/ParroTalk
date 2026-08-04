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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final TagRepository tagRepository;
    private final AdminTagMapper tagMapper;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminTagDto> searchTags(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Tag> tagPage = tagRepository.findAll(pageable);

        List<AdminTagDto> content = tagPage.getContent().stream().map(t -> {
            AdminTagDto dto = tagMapper.toDto(t);
            dto.setLessonsCount(tagRepository.countLessonsByTagId(t.getId()));
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
            slug = request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-");
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

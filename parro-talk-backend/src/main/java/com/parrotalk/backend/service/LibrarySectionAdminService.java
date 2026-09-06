package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.CacheNames;
import com.parrotalk.backend.constant.SectionType;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionRequest;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionResponse;
import com.parrotalk.backend.dto.admin.ReorderLibrarySectionsRequest;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.LibrarySection;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.LibrarySectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LibrarySectionAdminService {

    private final LibrarySectionRepository librarySectionRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<AdminLibrarySectionResponse> getAllSections() {
        return librarySectionRepository.findAllByOrderByOrderIndexAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = CacheNames.LIBRARY_SECTIONS_CACHE, allEntries = true)
    public AdminLibrarySectionResponse createSection(AdminLibrarySectionRequest request) {
        Category category = null;
        String program = null;

        if (request.getType() == SectionType.CATEGORY) {
            if (request.getCategoryId() == null) {
                throw new ParroTalkException("Category ID is required for CATEGORY section type", HttpStatus.BAD_REQUEST);
            }
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ParroTalkException("Category not found with id: " + request.getCategoryId(), HttpStatus.NOT_FOUND));
        } else if (request.getType() == SectionType.PROGRAM) {
            if (request.getProgram() == null || request.getProgram().isBlank()) {
                throw new ParroTalkException("Program is required for PROGRAM section type", HttpStatus.BAD_REQUEST);
            }
            program = request.getProgram().trim();
        }

        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null) {
            List<LibrarySection> existing = librarySectionRepository.findAllByOrderByOrderIndexAsc();
            orderIndex = existing.isEmpty() ? 0 :
                    existing.stream()
                            .mapToInt(s -> s.getOrderIndex() != null ? s.getOrderIndex() : 0)
                            .max()
                            .orElse(-1) + 1;
        }

        LibrarySection section = LibrarySection.builder()
                .title(request.getTitle().trim())
                .type(request.getType())
                .category(category)
                .program(program)
                .orderIndex(orderIndex)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .maxItems(request.getMaxItems() != null && request.getMaxItems() > 0 ? request.getMaxItems() : 10)
                .build();

        LibrarySection saved = librarySectionRepository.save(section);
        log.info("Created library section with ID: {}, type: {}", saved.getId(), saved.getType());
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.LIBRARY_SECTIONS_CACHE, allEntries = true)
    public AdminLibrarySectionResponse updateSection(UUID id, AdminLibrarySectionRequest request) {
        LibrarySection section = librarySectionRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException("Library section not found with id: " + id, HttpStatus.NOT_FOUND));

        Category category = null;
        String program = null;

        if (request.getType() == SectionType.CATEGORY) {
            if (request.getCategoryId() == null) {
                throw new ParroTalkException("Category ID is required for CATEGORY section type", HttpStatus.BAD_REQUEST);
            }
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ParroTalkException("Category not found with id: " + request.getCategoryId(), HttpStatus.NOT_FOUND));
        } else if (request.getType() == SectionType.PROGRAM) {
            if (request.getProgram() == null || request.getProgram().isBlank()) {
                throw new ParroTalkException("Program is required for PROGRAM section type", HttpStatus.BAD_REQUEST);
            }
            program = request.getProgram().trim();
        }

        section.setTitle(request.getTitle().trim());
        section.setType(request.getType());
        section.setCategory(category);
        section.setProgram(program);

        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }
        if (request.getIsActive() != null) {
            section.setIsActive(request.getIsActive());
        }
        if (request.getMaxItems() != null && request.getMaxItems() > 0) {
            section.setMaxItems(request.getMaxItems());
        }

        LibrarySection saved = librarySectionRepository.save(section);
        log.info("Updated library section with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.LIBRARY_SECTIONS_CACHE, allEntries = true)
    public void deleteSection(UUID id) {
        LibrarySection section = librarySectionRepository.findById(id)
                .orElseThrow(() -> new ParroTalkException("Library section not found with id: " + id, HttpStatus.NOT_FOUND));
        librarySectionRepository.delete(section);
        log.info("Deleted library section with ID: {}", id);
    }

    @Transactional
    @CacheEvict(value = CacheNames.LIBRARY_SECTIONS_CACHE, allEntries = true)
    public void reorderSections(ReorderLibrarySectionsRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ParroTalkException("Items list cannot be empty", HttpStatus.BAD_REQUEST);
        }

        List<LibrarySection> toUpdate = new ArrayList<>();
        for (ReorderLibrarySectionsRequest.ReorderSectionItem item : request.getItems()) {
            LibrarySection section = librarySectionRepository.findById(item.getId())
                    .orElseThrow(() -> new ParroTalkException("Library section not found with id: " + item.getId(), HttpStatus.NOT_FOUND));
            section.setOrderIndex(item.getOrderIndex());
            toUpdate.add(section);
        }

        librarySectionRepository.saveAll(toUpdate);
        log.info("Reordered {} library sections", toUpdate.size());
    }

    public AdminLibrarySectionResponse mapToResponse(LibrarySection section) {
        return AdminLibrarySectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .type(section.getType())
                .categoryId(section.getCategory() != null ? section.getCategory().getId() : null)
                .categoryName(section.getCategory() != null ? section.getCategory().getName() : null)
                .categorySlug(section.getCategory() != null ? section.getCategory().getSlug() : null)
                .program(section.getProgram())
                .orderIndex(section.getOrderIndex())
                .isActive(section.getIsActive())
                .maxItems(section.getMaxItems())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}

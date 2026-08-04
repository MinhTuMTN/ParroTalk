package com.parrotalk.backend.service.cms;

import com.parrotalk.backend.dto.admin.*;
import com.parrotalk.backend.entity.Category;
import com.parrotalk.backend.entity.Tag;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.cms.*;
import com.parrotalk.backend.repository.CategoryRepository;
import com.parrotalk.backend.repository.TagRepository;
import com.parrotalk.backend.repository.cms.AdminVocabularyRepository;
import com.parrotalk.backend.repository.cms.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminVocabularyService {

    private final AdminVocabularyRepository vocabularyRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AdminVocabularyListDto> getVocabularies(Specification<AdminVocabulary> spec, Pageable pageable) {
        return vocabularyRepository.findAll(spec, pageable).map(this::toListDto);
    }

    @Transactional(readOnly = true)
    public AdminVocabularyResponseDto getVocabulary(UUID id) {
        AdminVocabulary vocab = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary not found"));
        return toResponseDto(vocab);
    }

    @Transactional
    public AdminVocabularyResponseDto createVocabulary(AdminVocabularyRequestDto request, User user) {
        if (vocabularyRepository.existsByWordAndPartOfSpeechAndCefrLevel(
                request.getWord(), request.getPartOfSpeech(), request.getCefrLevel())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vocabulary already exists");
        }

        AdminVocabulary vocab = new AdminVocabulary();
        updateVocabFromRequest(vocab, request);
        vocab.setCreatedBy(user);
        vocab.setUpdatedBy(user);

        AdminVocabulary saved = vocabularyRepository.save(vocab);
        logAction("AdminVocabulary", saved.getId(), "CREATE", user);

        return toResponseDto(saved);
    }

    @Transactional
    public AdminVocabularyResponseDto updateVocabulary(UUID id, AdminVocabularyRequestDto request, User user) {
        AdminVocabulary vocab = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary not found"));

        // Check duplicates if changing key fields
        if ((!vocab.getWord().equals(request.getWord()) ||
             !vocab.getPartOfSpeech().equals(request.getPartOfSpeech()) ||
             !vocab.getCefrLevel().equals(request.getCefrLevel())) &&
             vocabularyRepository.existsByWordAndPartOfSpeechAndCefrLevel(request.getWord(), request.getPartOfSpeech(), request.getCefrLevel())) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "Update results in a duplicate vocabulary");
        }

        updateVocabFromRequest(vocab, request);
        vocab.setUpdatedBy(user);

        AdminVocabulary saved = vocabularyRepository.save(vocab);
        logAction("AdminVocabulary", saved.getId(), "UPDATE", user);

        return toResponseDto(saved);
    }

    @Transactional
    public void deleteVocabulary(UUID id, User user) {
        AdminVocabulary vocab = vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary not found"));
        
        vocabularyRepository.delete(vocab);
        logAction("AdminVocabulary", id, "DELETE", user);
    }

    private void updateVocabFromRequest(AdminVocabulary vocab, AdminVocabularyRequestDto request) {
        vocab.setWord(request.getWord());
        vocab.setIpaUk(request.getIpaUk());
        vocab.setIpaUs(request.getIpaUs());
        vocab.setAudioUk(request.getAudioUk());
        vocab.setAudioUs(request.getAudioUs());
        vocab.setCefrLevel(request.getCefrLevel());
        vocab.setFrequencyRank(request.getFrequencyRank());
        vocab.setPartOfSpeech(request.getPartOfSpeech());
        vocab.setImageUrl(request.getImageUrl());
        vocab.setNotes(request.getNotes());
        vocab.setSource(request.getSource());
        vocab.setStatus(request.getStatus());

        // Definitions
        if (vocab.getDefinitions() != null) {
            vocab.getDefinitions().clear();
        } else {
            vocab.setDefinitions(new java.util.ArrayList<>());
        }
        if (request.getDefinitions() != null) {
            for (AdminDefinitionDto dDto : request.getDefinitions()) {
                AdminVocabularyDefinition d = new AdminVocabularyDefinition();
                d.setVocabulary(vocab);
                d.setDefinition(dDto.getDefinition());
                d.setEnglishDefinition(dDto.getEnglishDefinition());
                d.setVietnameseDefinition(dDto.getVietnameseDefinition());
                d.setDisplayOrder(dDto.getDisplayOrder() != null ? dDto.getDisplayOrder() : 0);
                vocab.getDefinitions().add(d);
            }
        }

        // Examples
        if (vocab.getExamples() != null) {
            vocab.getExamples().clear();
        } else {
            vocab.setExamples(new java.util.ArrayList<>());
        }
        if (request.getExamples() != null) {
            for (AdminExampleDto eDto : request.getExamples()) {
                AdminVocabularyExample e = new AdminVocabularyExample();
                e.setVocabulary(vocab);
                e.setSentence(eDto.getSentence());
                e.setTranslation(eDto.getTranslation());
                e.setDisplayOrder(eDto.getDisplayOrder() != null ? eDto.getDisplayOrder() : 0);
                vocab.getExamples().add(e);
            }
        }

        // Relations
        if (vocab.getRelations() != null) {
            vocab.getRelations().clear();
        } else {
            vocab.setRelations(new java.util.ArrayList<>());
        }
        if (request.getRelations() != null) {
            for (AdminRelationDto rDto : request.getRelations()) {
                AdminVocabularyRelation r = new AdminVocabularyRelation();
                r.setVocabulary(vocab);
                r.setRelationType(rDto.getRelationType());
                r.setRelatedWord(rDto.getRelatedWord());
                r.setDisplayOrder(rDto.getDisplayOrder() != null ? rDto.getDisplayOrder() : 0);
                vocab.getRelations().add(r);
            }
        }

        // Categories
        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            categories.addAll(categoryRepository.findAllById(request.getCategoryIds()));
        }
        vocab.setCategories(categories);

        // Tags
        Set<Tag> tags = new HashSet<>();
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<Tag> existingTags = tagRepository.findByNameIn(request.getTags());
            Set<String> existingNames = existingTags.stream().map(Tag::getName).collect(Collectors.toSet());
            
            for (String tagName : request.getTags()) {
                if (!existingNames.contains(tagName)) {
                    existingTags.add(tagRepository.save(Tag.builder().name(tagName).build()));
                }
            }
            tags.addAll(existingTags);
        }
        vocab.setTags(tags);
    }

    private AdminVocabularyListDto toListDto(AdminVocabulary vocab) {
        return AdminVocabularyListDto.builder()
                .id(vocab.getId())
                .word(vocab.getWord())
                .partOfSpeech(vocab.getPartOfSpeech())
                .cefrLevel(vocab.getCefrLevel())
                .definitionsCount(vocab.getDefinitions() != null ? vocab.getDefinitions().size() : 0)
                .examplesCount(vocab.getExamples() != null ? vocab.getExamples().size() : 0)
                .hasAudio(vocab.getAudioUk() != null || vocab.getAudioUs() != null)
                .status(vocab.getStatus())
                .updatedAt(vocab.getUpdatedAt())
                .build();
    }

    private AdminVocabularyResponseDto toResponseDto(AdminVocabulary vocab) {
        return AdminVocabularyResponseDto.builder()
                .id(vocab.getId())
                .word(vocab.getWord())
                .ipaUk(vocab.getIpaUk())
                .ipaUs(vocab.getIpaUs())
                .audioUk(vocab.getAudioUk())
                .audioUs(vocab.getAudioUs())
                .cefrLevel(vocab.getCefrLevel())
                .frequencyRank(vocab.getFrequencyRank())
                .partOfSpeech(vocab.getPartOfSpeech())
                .imageUrl(vocab.getImageUrl())
                .notes(vocab.getNotes())
                .source(vocab.getSource())
                .status(vocab.getStatus())
                .createdAt(vocab.getCreatedAt())
                .updatedAt(vocab.getUpdatedAt())
                .definitions(vocab.getDefinitions() == null ? List.of() : vocab.getDefinitions().stream().map(d ->
                        AdminDefinitionDto.builder()
                                .id(d.getId())
                                .definition(d.getDefinition())
                                .englishDefinition(d.getEnglishDefinition())
                                .vietnameseDefinition(d.getVietnameseDefinition())
                                .displayOrder(d.getDisplayOrder())
                                .build()
                ).collect(Collectors.toList()))
                .examples(vocab.getExamples() == null ? List.of() : vocab.getExamples().stream().map(e ->
                        AdminExampleDto.builder()
                                .id(e.getId())
                                .sentence(e.getSentence())
                                .translation(e.getTranslation())
                                .displayOrder(e.getDisplayOrder())
                                .build()
                ).collect(Collectors.toList()))
                .relations(vocab.getRelations() == null ? List.of() : vocab.getRelations().stream().map(r ->
                        AdminRelationDto.builder()
                                .id(r.getId())
                                .relationType(r.getRelationType())
                                .relatedWord(r.getRelatedWord())
                                .displayOrder(r.getDisplayOrder())
                                .build()
                ).collect(Collectors.toList()))
                .categories(vocab.getCategories() == null ? Set.of() : vocab.getCategories().stream().map(Category::getName).collect(Collectors.toSet()))
                .tags(vocab.getTags() == null ? Set.of() : vocab.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                .build();
    }

    private void logAction(String entityName, UUID entityId, String action, User user) {
        AuditLog log = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .createdBy(user)
                .build();
        auditLogRepository.save(log);
    }
}

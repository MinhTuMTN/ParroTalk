package com.parrotalk.backend.service.cms;

import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.dto.admin.AdminTagCreateRequest;
import com.parrotalk.backend.dto.admin.AdminTagDto;
import com.parrotalk.backend.entity.Tag;
import com.parrotalk.backend.mapper.admin.AdminTagMapper;
import com.parrotalk.backend.repository.TagRepository;
import com.parrotalk.backend.repository.cms.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private AdminTagMapper tagMapper;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminTagService tagService;

    private AdminTagCreateRequest createRequest;
    private Tag tag;
    private AdminTagDto tagDto;
    private UUID tagId;

    @BeforeEach
    void setUp() {
        tagId = UUID.randomUUID();
        
        createRequest = new AdminTagCreateRequest();
        createRequest.setName("Test Tag");
        createRequest.setSlug("test-tag");
        createRequest.setStatus(CmsItemStatus.ACTIVE);

        tag = Tag.builder()
                .name("Test Tag")
                .slug("test-tag")
                .status(CmsItemStatus.ACTIVE)
                .build();
        tag.setId(tagId);

        tagDto = new AdminTagDto();
        tagDto.setId(tagId);
        tagDto.setName("Test Tag");
        tagDto.setSlug("test-tag");
    }

    @Test
    void createTag_Success() {
        when(tagRepository.existsBySlug(createRequest.getSlug())).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(tag);
        when(tagMapper.toDto(any(Tag.class))).thenReturn(tagDto);

        AdminTagDto result = tagService.createTag(createRequest);

        assertNotNull(result);
        assertEquals(tagDto.getName(), result.getName());
        
        verify(tagRepository, times(1)).save(any(Tag.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void deleteTag_Success() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(tagRepository.countLessonsByTagId(tagId)).thenReturn(0L);

        tagService.deleteTag(tagId);

        verify(tagRepository, times(1)).delete(tag);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void deleteTag_InUse_ThrowsException() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(tagRepository.countLessonsByTagId(tagId)).thenReturn(10L);

        assertThrows(IllegalStateException.class, () -> {
            tagService.deleteTag(tagId);
        });
        
        verify(tagRepository, never()).delete(any(Tag.class));
    }
}

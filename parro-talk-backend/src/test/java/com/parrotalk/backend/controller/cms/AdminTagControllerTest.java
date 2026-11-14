package com.parrotalk.backend.controller.cms;

import com.parrotalk.backend.constant.CmsItemStatus;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.admin.AdminTagCreateRequest;
import com.parrotalk.backend.dto.admin.AdminTagDto;
import com.parrotalk.backend.dto.admin.AdminTagUpdateRequest;
import com.parrotalk.backend.service.cms.AdminTagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTagControllerTest {

    @Mock
    private AdminTagService tagService;

    @InjectMocks
    private AdminTagController tagController;

    @Test
    void getTags_ReturnsFilteredPage() {
        AdminTagDto tag = AdminTagDto.builder()
                .id(UUID.randomUUID())
                .name("Grammar")
                .slug("grammar")
                .status(CmsItemStatus.ACTIVE)
                .build();

        PageResponse<AdminTagDto> pageResponse = PageResponse.<AdminTagDto>builder()
                .content(List.of(tag))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(tagService.searchTags("gram", CmsItemStatus.ACTIVE, 0, 10)).thenReturn(pageResponse);

        ResponseEntity<ApiResponse<PageResponse<AdminTagDto>>> response =
                tagController.getTags("gram", CmsItemStatus.ACTIVE, 0, 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResult().getContent().size());
        assertEquals("Grammar", response.getBody().getResult().getContent().get(0).getName());

        verify(tagService).searchTags("gram", CmsItemStatus.ACTIVE, 0, 10);
    }

    @Test
    void createTag_ReturnsCreatedDto() {
        AdminTagCreateRequest request = new AdminTagCreateRequest();
        request.setName("Vocabulary");
        request.setSlug("vocabulary");
        request.setColor("#10B981");
        request.setStatus(CmsItemStatus.ACTIVE);

        AdminTagDto dto = AdminTagDto.builder()
                .id(UUID.randomUUID())
                .name("Vocabulary")
                .slug("vocabulary")
                .color("#10B981")
                .status(CmsItemStatus.ACTIVE)
                .build();

        when(tagService.createTag(request)).thenReturn(dto);

        ResponseEntity<ApiResponse<AdminTagDto>> response = tagController.createTag(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Vocabulary", response.getBody().getResult().getName());

        verify(tagService).createTag(request);
    }
}

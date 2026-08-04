package com.parrotalk.backend.mapper.admin;

import com.parrotalk.backend.dto.admin.AdminCategoryDto;
import com.parrotalk.backend.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminCategoryMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "slug", source = "slug")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "icon", source = "icon")
    @Mapping(target = "color", source = "color")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "parentCategoryId", source = "parentCategoryId")
    @Mapping(target = "sortOrder", source = "sortOrder")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    @Mapping(target = "lessonsCount", ignore = true) // Set manually in service
    AdminCategoryDto toDto(Category category);
}

package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Category mapper.
 * 
 * @author MinhTuMTN
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Convert category to category response.
     * 
     * @param category Category entity
     * @return Category response
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    CategoryResponse toCategoryResponse(Category category);
}

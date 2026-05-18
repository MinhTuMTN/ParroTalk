package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.entity.Category;
import org.mapstruct.Mapper;

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
    CategoryResponse toCategoryResponse(Category category);
}

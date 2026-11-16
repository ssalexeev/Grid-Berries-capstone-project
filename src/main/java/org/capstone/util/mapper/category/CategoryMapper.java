package org.capstone.util.mapper.category;


import org.capstone.domain.Category;
import org.capstone.dto.request.CategoryRequest;
import org.capstone.dto.response.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = ParentCategoryMapper.class)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(
            source = "parentCategoryId",
            target = "parentCategory",
            qualifiedByName = "mapCategoryId"
    )
    @Mapping(target = "createdAt", ignore = true)
    Category toEntity(CategoryRequest request);

    CategoryResponse toResponse(Category category);
}

package org.capstone.util.mapper.category;


import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.service.CategoryService;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParentCategoryMapper {
    private final CategoryService categoryService;

    @Named("mapCategoryId")
    public Category mapParentCategoryById(Long parentCategoryId){
        return categoryService.getCategory(parentCategoryId);
    }

}

package org.capstone.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.dto.request.CategoryRequest;
import org.capstone.dto.response.CategoryResponse;
import org.capstone.service.CategoryService;
import org.capstone.util.mapper.category.CategoryMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoriesController {
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        List<CategoryResponse> rootCategories = categoryService.getRootCategories().stream()
                .map(categoryMapper::toResponse)
                .toList();

        return ResponseEntity.ok(rootCategories);
    }

    @GetMapping("/parent/{parentCategoryId}")
    public ResponseEntity<List<CategoryResponse>> getChildrenCategories(@PathVariable Long parentCategoryId) {
        List<CategoryResponse> childrenCategoriesResponse = categoryService.getChildren(parentCategoryId).stream()
                .map(categoryMapper::toResponse)
                .toList();


        return ResponseEntity.ok(childrenCategoriesResponse);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        Category persistedCategory = categoryService.createCategory(category);

        return ResponseEntity.ok(categoryMapper.toResponse(persistedCategory));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        Category category = categoryService.getCategory(categoryId);

        return ResponseEntity.ok(categoryMapper.toResponse(category));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> patchCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);

        Category patchedCategory = categoryService.patchCategory(categoryId, category);

        return ResponseEntity.ok(categoryMapper.toResponse(patchedCategory));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}

package org.capstone.service;

import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Category;
import org.capstone.repository.jpa.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(Category category) {
        validateParent(category.getParentCategoryId(), null);

        category.setId(null);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category patchCategory(Long categoryId, Category category) {
        Category existing = getCategory(categoryId);


        validateParent(category.getParentCategoryId(), categoryId);

        if (category.getName() != null){
            existing.setName(category.getName());
        }
        if (category.getDescription() != null){
            existing.setDescription(category.getDescription());
        }
        if (category.getParentCategoryId() != null){
            validateParent(category.getParentCategoryId(), existing.getId());
            existing.setParentCategory(category.getParentCategory());
        }

        return categoryRepository.save(category);
    }

    public Category getCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID cannot be null.");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findRootCategories();
    }

    public List<Category> getChildren(Long parentCategoryId) {
        if (parentCategoryId != null) {
            getCategory(parentCategoryId);
        }
        return categoryRepository.findByParentCategoryId(parentCategoryId);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = getCategory(categoryId);
        for (Category child : categoryRepository.findByParentCategoryId(categoryId)) {
            child.setParentCategoryId(category.getParentCategoryId());
            categoryRepository.save(child);
        }
        categoryRepository.delete(category);
    }

    private CategoryNode toNode(Category category, List<Category> categories, Set<Long> ancestors) {
        if (category.getId() == null || !ancestors.add(category.getId())) {
            throw new IllegalStateException("Category hierarchy contains a cycle.");
        }
        List<CategoryNode> children = categories.stream()
                .filter(candidate -> category.getId().equals(candidate.getParentCategoryId()))
                .map(child -> toNode(child, categories, new HashSet<>(ancestors)))
                .toList();
        return new CategoryNode(category, children);
    }

    private void validateParent(Long parentCategoryId, Long categoryId) {
        if (parentCategoryId == null) {
            return;
        }
        Category parent = getCategory(parentCategoryId);
        Set<Long> visited = new HashSet<>();
        Category current = parent;
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new EntityExistsException("Existing category hierarchy contains a cycle.");
            }
            if (current.getId().equals(categoryId)) {
                throw new IllegalArgumentException("A category cannot be its own descendant.");
            }
            current = current.getParentCategoryId() == null
                    ? null
                    : getCategory(current.getParentCategoryId());
        }
    }

    public record CategoryNode(Category category, List<CategoryNode> children) {
        public CategoryNode {
            children = List.copyOf(children);
        }
    }
}

package org.capstone.service;

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
        validateCategory(category);
        validateParent(category.getParentCategoryId(), null);
        category.setId(null);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long categoryId, String name, String description, Long parentCategoryId) {
        Category category = getCategory(categoryId);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        validateParent(parentCategoryId, categoryId);
        category.setName(name);
        category.setDescription(description);
        category.setParentCategoryId(parentCategoryId);
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
        return categoryRepository.findByParentCategoryId(null);
    }

    public List<Category> getChildren(Long parentCategoryId) {
        if (parentCategoryId != null) {
            getCategory(parentCategoryId);
        }
        return categoryRepository.findByParentCategoryId(parentCategoryId);
    }

    public List<CategoryNode> getCategoryTree() {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryNode> roots = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParentCategoryId() == null) {
                roots.add(toNode(category, categories, new HashSet<>()));
            }
        }
        return List.copyOf(roots);
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

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null.");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
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
                throw new IllegalStateException("Existing category hierarchy contains a cycle.");
            }
            if (current.getId().equals(categoryId)) {
                throw new IllegalArgumentException("A category cannot be its own descendant.");
            }
            current = current.getParentCategoryId() == null
                    ? null
                    : categoryRepository.findById(current.getParentCategoryId()).orElseThrow(
                            () -> new IllegalStateException("Parent category does not exist."));
        }
    }

    public record CategoryNode(Category category, List<CategoryNode> children) {
        public CategoryNode {
            children = List.copyOf(children);
        }
    }
}

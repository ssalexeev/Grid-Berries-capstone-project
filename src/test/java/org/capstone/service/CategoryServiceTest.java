package org.capstone.service;

import org.capstone.domain.Category;
import org.capstone.repository.jpa.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void patchCategoryRejectsHierarchyCycles() {
        Category current = Category.builder().id(1L).name("Fruit").build();
        Category descendant = Category.builder().id(2L).name("Berries")
                .parentCategory(Category.builder().id(1L).build())
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(current));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(descendant));

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.patchCategory(1L, descendant));
        verify(categoryRepository, org.mockito.Mockito.never()).save(current);
    }

    @Test
    void patchCategoryUpdatesTheExistingCategoryAndParent() {
        Category current = Category.builder().id(1L).name("Fruit").description("Old").build();
        Category newParent = Category.builder().id(2L).name("Food").build();
        Category patch = Category.builder().name("Fresh fruit").parentCategory(newParent).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(current));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newParent));
        when(categoryRepository.save(current)).thenReturn(current);

        Category updated = categoryService.patchCategory(1L, patch);

        assertEquals("Fresh fruit", updated.getName());
        assertEquals(2L, updated.getParentCategoryId());
        verify(categoryRepository).save(current);
    }
}

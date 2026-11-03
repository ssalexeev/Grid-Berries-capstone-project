package org.capstone.service;

import org.capstone.domain.Category;
import org.capstone.repository.jdbc.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
    void updateCategoryRejectsHierarchyCycles() {
        Category current = Category.builder().id(1L).name("Fruit").build();
        Category descendant = Category.builder().id(2L).name("Berries")
                .parentCategory(Category.builder().id(1L).build())
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(current));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(descendant));

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.updateCategory(1L, "Fruit", null, 2L));
        verify(categoryRepository, org.mockito.Mockito.never()).save(current);
    }
}

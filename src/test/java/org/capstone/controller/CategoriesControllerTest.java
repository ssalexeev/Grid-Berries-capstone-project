package org.capstone.controller;

import org.capstone.domain.Category;
import org.capstone.dto.request.CategoryRequest;
import org.capstone.dto.response.CategoryResponse;
import org.capstone.service.CategoryService;
import org.capstone.util.mapper.category.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriesController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriesControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CategoryService categoryService;
    @MockitoBean CategoryMapper categoryMapper;

    @Test
    void getRootCategories_ReturnsMappedCategories() throws Exception {
        Category category = Category.builder().id(1L).name("Fruits").build();
        when(categoryService.getRootCategories()).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(1L, "Fruits", null));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Fruits"));
    }

    @Test
    void getChildren_ReturnsMappedCategories() throws Exception {
        when(categoryService.getChildren(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories/parent/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createCategory_ReturnsCreatedCategory() throws Exception {
        Category mapped = Category.builder().name("Fresh").build();
        Category saved = Category.builder().id(3L).name("Fresh").build();
        when(categoryMapper.toEntity(any(CategoryRequest.class))).thenReturn(mapped);
        when(categoryService.createCategory(mapped)).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(new CategoryResponse(3L, "Fresh", null));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Fresh","description":"Fresh food"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Fresh"));
    }

    @Test
    void getCategory_ReturnsMappedCategory() throws Exception {
        Category category = Category.builder().id(4L).name("Vegetables").build();
        when(categoryService.getCategory(4L)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(new CategoryResponse(4L, "Vegetables", null));

        mockMvc.perform(get("/api/v1/categories/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vegetables"));
    }

    @Test
    void patchCategory_ReturnsMappedCategory() throws Exception {
        Category patch = Category.builder().name("Updated").build();
        Category saved = Category.builder().id(4L).name("Updated").build();
        when(categoryMapper.toEntity(any(CategoryRequest.class))).thenReturn(patch);
        when(categoryService.patchCategory(4L, patch)).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(new CategoryResponse(4L, "Updated", null));

        mockMvc.perform(patch("/api/v1/categories/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteCategory_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/4"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(4L);
    }
}

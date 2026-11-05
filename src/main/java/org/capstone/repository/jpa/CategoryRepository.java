package org.capstone.repository.jpa;

import org.capstone.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("""
            select category
            from Category category
            where (:parentCategoryId is null and category.parentCategory is null)
               or category.parentCategory.id = :parentCategoryId
            """)
    List<Category> findByParentCategoryId(@Param("parentCategoryId") Long parentCategoryId);

    @Query("""
            select category
            from Category category
            join category.products product
            where product.id = :productId
            order by category.id
            """)
    List<Category> findByProductId(@Param("productId") Long productId);
}

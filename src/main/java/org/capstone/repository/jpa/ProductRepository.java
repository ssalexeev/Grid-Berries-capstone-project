package org.capstone.repository.jpa;

import org.capstone.domain.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findAllBySellerProfileId(Long sellerProfileId);

    @EntityGraph(attributePaths = {"seller", "categories"})
    Slice<Product> findBy(Specification<Product> specification, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            update Product product
            set product.stockQuantity = product.stockQuantity - :quantity
            where product.id = :productId and product.stockQuantity >= :quantity
            """)
    int reserveStockIfAvailable(
            @Param("productId") Long productId,
            @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("""
            update Product product
            set product.stockQuantity = product.stockQuantity + :quantity
            where product.id = :productId
            """)
    int restoreStockQuantity(
            @Param("productId") Long productId,
            @Param("quantity") int quantity);
}

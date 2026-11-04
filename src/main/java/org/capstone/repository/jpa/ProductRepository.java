package org.capstone.repository.jpa;

import org.capstone.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllBySellerProfileId(Long sellerProfileId);

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

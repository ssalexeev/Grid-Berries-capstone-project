package org.capstone.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.ProductType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private ProductType type;
    private ProductStatus status;

    // Relationships
    private SellerProfile sellerProfile;
    private Category category;
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    public void updateStock(Integer quantity) {
        this.stockQuantity = quantity;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }
}

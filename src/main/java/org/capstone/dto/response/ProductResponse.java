package org.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.capstone.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private ProductStatus  status;

    private Seller seller;
    private List<Category> categories;

    private LocalDateTime createdAt;


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Seller{
        private Long id;
        private String storeName;

    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category{
        private Long id;
        private String name;

    }
}

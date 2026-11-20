package org.capstone.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CartItemResponse{
        private Long id;
        private ProductResponse product;
        private Integer quantity;
        private LocalDateTime updatedAt;
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductResponse{
        private Long id;
        private String title;
        private BigDecimal price;
        private String imageUrl;
    }
}

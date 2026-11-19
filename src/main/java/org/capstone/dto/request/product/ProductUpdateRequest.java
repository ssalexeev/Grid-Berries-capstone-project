package org.capstone.dto.request.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdateRequest(
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
        BigDecimal price,

        @Min(value = 0, message = "Stock quantity must be greater than or equal to 0")
        Integer stockQuantity,

        @URL(message = "Image URL must be a valid URL")
        String imageUrl,

        List<Long> categoryIds
) {
}

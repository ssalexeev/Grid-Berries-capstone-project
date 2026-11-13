package org.capstone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfileCreateRequest {

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 100, message = "Store name must be between 2 and 100 characters")
    private String storeName;

    @Size(max = 1000, message = "Store description must not exceed 1000 characters")
    private String storeDescription;

    @NotBlank(message = "Contact information is required")
    @Size(max = 100, message = "Contact information must not exceed 255 characters")
    private String contactInfo;
}
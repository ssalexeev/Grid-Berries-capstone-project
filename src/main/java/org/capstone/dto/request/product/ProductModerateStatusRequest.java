package org.capstone.dto.request.product;

import lombok.Getter;
import lombok.Setter;
import org.capstone.domain.enums.ProductStatus;

@Getter
@Setter
public class ProductModerateStatusRequest {
    private ProductStatus status;
}

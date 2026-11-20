package org.capstone.util.mapper.cart;


import lombok.RequiredArgsConstructor;
import org.capstone.domain.Product;
import org.capstone.service.ProductService;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductService productService;

    @Named("mapProduct")
    public Product mapProduct(Long productId){
        return productService.getProductById(productId);
    }
}

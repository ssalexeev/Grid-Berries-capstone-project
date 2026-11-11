package org.capstone.controller.product.seller;


import lombok.RequiredArgsConstructor;
import org.capstone.domain.Product;
import org.capstone.dto.response.ProductResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.ProductService;
import org.capstone.util.mapper.product.ProductMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;
    private final AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(){
        Long sellerId = authenticationService.getCurrentSellerId();
        List<Product> products = productService.getProductsBySeller(sellerId);

        return ResponseEntity.ok(products.stream().map(productMapper::toResponse).toList());

    }
}

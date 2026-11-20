package org.capstone.controller.product;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Product;
import org.capstone.dto.request.product.ProductCreateRequest;
import org.capstone.dto.request.product.ProductModerateStatusRequest;
import org.capstone.dto.response.ProductResponse;
import org.capstone.dto.response.SliceResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.ProductService;
import org.capstone.service.search.ProductSearchCriteria;
import org.capstone.util.mapper.product.ProductMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductsController {
    private final ProductService productService;
    private final AuthenticationService authenticationService;
    private final ProductMapper productMapper;


    @GetMapping
    public ResponseEntity<SliceResponse<ProductResponse>> getProducts(
            @ModelAttribute ProductSearchCriteria criteria,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Slice<ProductResponse> slice = productService.getProductsSlice(criteria, pageable)
                .map(productMapper::toResponse);

        return ResponseEntity.ok(new SliceResponse<>(slice));
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long sellerId = authenticationService.getSellerIdFromAuthenticationContext();
        Product productEntity = productMapper.toEntity(request);

        Product createProduct = productService.createProduct(sellerId, productEntity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createProduct.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(productMapper.toResponse(createProduct));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);

        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductCreateRequest request) {
        Product product = productMapper.toEntity(request);

        Product updateProduct = productService.updateProduct(authenticationService.getSellerIdFromAuthenticationContext(), productId, product);

        return ResponseEntity.ok(productMapper.toResponse(updateProduct));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> patchProduct(@PathVariable Long productId, @Valid @RequestBody ProductCreateRequest request) {
        Product patchData = productMapper.toEntity(request);
        Product patchProduct = productService.patchProduct(productId, patchData);

        return ResponseEntity.ok(productMapper.toResponse(patchProduct));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long productId) {
        Long sellerId = authenticationService.getSellerIdFromAuthenticationContext();
        productService.deleteProduct(sellerId, productId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body("Product Deleted");
    }

    @PatchMapping("/{productId}/moderation")
    public ResponseEntity<ProductResponse> patchProductStatus(@PathVariable Long productId, @RequestBody ProductModerateStatusRequest request) {
        Product product = productService.moderateProductStatus(productId, request.getStatus());

        return ResponseEntity.ok(productMapper.toResponse(product));
    }

}

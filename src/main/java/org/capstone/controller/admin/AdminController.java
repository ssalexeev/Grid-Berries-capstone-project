package org.capstone.controller.admin;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.capstone.domain.Product;
import org.capstone.domain.User;
import org.capstone.dto.request.UserModerationRequest;
import org.capstone.dto.request.product.ProductUpdateRequest;
import org.capstone.dto.response.ProductResponse;
import org.capstone.dto.response.user.UserModerationResponse;
import org.capstone.security.AuthenticationService;
import org.capstone.service.ProductService;
import org.capstone.service.UserService;
import org.capstone.util.mapper.product.ProductMapper;
import org.capstone.util.mapper.user.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final ProductService productService;

    private final ProductMapper productMapper;

    @GetMapping
    public ResponseEntity<UserModerationResponse> getUserByEmailForModeration(@RequestParam String userEmail) {
        User user = userService.getUserByEmail(userEmail);

        return ResponseEntity.ok(userMapper.toModerateResponse(user));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserModerationResponse> moderateUser(@PathVariable Long userId, @Valid @RequestBody UserModerationRequest request) {
        User user = userMapper.toEntity(request);

        User patchedUser = userService.patchUser(userId, user);

        return ResponseEntity.ok(userMapper.toModerateResponse(patchedUser));
    }

    @PatchMapping("/product/{productId}")
    public ResponseEntity<ProductResponse> moderateProduct(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request) {
        //boolean isAdmin = authenticationService.getUserFromAuthenticationContext().getRole().equals(Role.ADMIN);
        Product patchData = productMapper.toEntity(request);
        Product patchProduct = productService.patchProduct(productId, patchData);

        return ResponseEntity.ok(productMapper.toResponse(patchProduct));
    }
}

package org.capstone.util.mapper.cart;


import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Product;
import org.capstone.dto.response.CartResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(source = "cartItems", target = "items")
    CartResponse toResponse(Cart cart);

    CartResponse.CartItemResponse toCartItemResponse(CartItem cartItem);
    CartResponse.ProductResponse toProductResponse(Product product);
}

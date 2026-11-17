package org.capstone.util.mapper.order;

import org.capstone.domain.Order;
import org.capstone.domain.OrderItem;
import org.capstone.domain.Product;
import org.capstone.dto.request.PlaceOrderRequest;
import org.capstone.dto.response.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    Order toEntity(PlaceOrderRequest placeOrderRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "product", source = "productId")
    OrderItem toEntity(PlaceOrderRequest.OrderItemRequest itemRequest);

    default Product toProduct(Long productId) {
        return productId == null ? null : Product.builder().id(productId).build();
    }

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productTitle", source = "product.title")
    OrderResponse.OrderItemResponse toResponse(OrderItem item);
}

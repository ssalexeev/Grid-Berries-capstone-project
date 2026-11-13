package org.capstone.util.mapper.product;

import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.dto.request.product.ProductCreateRequest;
import org.capstone.dto.response.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sellerProfile", ignore = true)
    @Mapping(target = "categories", source = "categoryIds")
    Product toEntity(ProductCreateRequest createRequest);

    @Mapping(target = "seller.id", source = "sellerProfile.id")
    @Mapping(target = "seller.storeName", source = "sellerProfile.storeName")
    @Mapping(target = "categories", source = "categories")
    ProductResponse toResponse(Product product);

    default Set<Category> mapCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null) {
            return null;
        }

        return categoryIds.stream()
                .filter(Objects::nonNull)
                .map(id -> Category.builder().id(id).build())
                .collect(Collectors.toSet());
    }

    default List<ProductResponse.Category> mapCategories(Set<Category> categories) {
        if (categories == null) {
            return null;
        }

        return categories.stream()
                .filter(Objects::nonNull)
                .map(this::mapCategory)
                .collect(Collectors.toList());
    }

    default ProductResponse.Category mapCategory(Category category) {
        if (category == null) {
            return null;
        }

        return ProductResponse.Category.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}

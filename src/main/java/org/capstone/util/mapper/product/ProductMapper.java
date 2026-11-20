package org.capstone.util.mapper.product;

import org.capstone.domain.Category;
import org.capstone.domain.Product;
import org.capstone.dto.request.product.ProductCreateRequest;
import org.capstone.dto.request.product.ProductUpdateRequest;
import org.capstone.dto.response.ProductResponse;
import org.capstone.util.mapper.CentralMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Mapper(config = CentralMapperConfig.class)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sellerProfile", ignore = true)
    @Mapping(target = "categories", source = "categoryIds")
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(ProductCreateRequest createRequest);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sellerProfile", ignore = true)
    @Mapping(target = "categories", source = "categoryIds")
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(ProductUpdateRequest updateRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerProfile", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "categories", ignore = true)
    void updateEntity(@MappingTarget Product existing, Product patchData);

    @Mapping(target = "seller.id", source = "sellerProfile.id")
    @Mapping(target = "seller.storeName", source = "sellerProfile.storeName")
    ProductResponse toResponse(Product product);


    ProductResponse.Category mapCategory(Category category);


    default Set<Category> mapCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null) {
            return null;
        }
        return categoryIds.stream()
                .filter(Objects::nonNull)
                .map(id -> Category.builder().id(id).build())
                .collect(Collectors.toSet());
    }
}

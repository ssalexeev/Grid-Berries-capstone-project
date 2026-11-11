package org.capstone.util.mapper.product;


import org.capstone.domain.Product;
import org.capstone.dto.request.product.ProductCreateRequest;
import org.capstone.dto.response.ProductResponse;
import org.capstone.util.mapper.user.PasswordMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PasswordMapper.class)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sellerProfile", ignore = true)
    Product toEntity(ProductCreateRequest createRequest);

    ProductResponse toResponse(Product product);
}

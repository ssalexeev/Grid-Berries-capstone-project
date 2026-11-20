package org.capstone.util.mapper;

import org.capstone.domain.SellerProfile;
import org.capstone.dto.request.SellerProfileCreateRequest;
import org.capstone.dto.response.SellerProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SellerProfileMapper {

    SellerProfile toEntity(SellerProfileCreateRequest request);

    SellerProfileResponse toResponse(SellerProfile profile);

}

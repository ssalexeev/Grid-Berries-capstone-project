package org.capstone.util.mapper.user;

import org.capstone.domain.User;
import org.capstone.dto.request.UserRegistrationRequest;
import org.capstone.dto.response.user.UserRegistrationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = PasswordMapper.class)
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(
            source = "password",
            target = "passwordHash",
            qualifiedByName = "encodePassword"
    )
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserRegistrationRequest userRegistrationRequest);


    UserRegistrationResponse toResponse(User user);
}

package org.capstone.util.mapper.user;

import org.capstone.domain.User;
import org.capstone.dto.request.UserModerationRequest;
import org.capstone.dto.request.UserRegistrationRequest;
import org.capstone.dto.response.user.UserModerationResponse;
import org.capstone.dto.response.user.UserRegistrationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        uses = PasswordMapper.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(
            source = "password",
            target = "passwordHash",
            qualifiedByName = "encodePassword"
    )
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserRegistrationRequest userRegistrationRequest);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(
            source = "password",
            target = "passwordHash",
            qualifiedByName = "encodePassword"
    )
    User toEntity(UserModerationRequest moderationRequest);

    UserRegistrationResponse toResponse(User user);

    UserModerationResponse toModerateResponse(User user);
}

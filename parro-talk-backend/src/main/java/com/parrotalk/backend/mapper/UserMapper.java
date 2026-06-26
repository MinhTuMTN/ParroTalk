package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "username", expression = "java(resolveUsername(user))")
    UserResponse toUserResponse(User user);

    default String resolveUsername(User user) {
        if (user.getDisplayUsername() != null && !user.getDisplayUsername().isBlank()) {
            return user.getDisplayUsername();
        }

        return user.getEmail().substring(0, user.getEmail().indexOf("@"));
    }
}

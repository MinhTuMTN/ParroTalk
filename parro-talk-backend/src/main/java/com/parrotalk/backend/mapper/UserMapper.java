package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "username", expression = "java(resolveUsername(user))")
    @Mapping(target = "hasPassword", expression = "java(resolveHasPassword(user))")
    UserResponse toUserResponse(User user);

    default String resolveUsername(User user) {
        if (user.getDisplayUsername() != null && !user.getDisplayUsername().isBlank()) {
            return user.getDisplayUsername();
        }

        int atIndex = user.getEmail() != null ? user.getEmail().indexOf("@") : -1;
        return atIndex > 0 ? user.getEmail().substring(0, atIndex) : user.getEmail();
    }

    default boolean resolveHasPassword(User user) {
        return user != null && user.getPassword() != null && !user.getPassword().isBlank();
    }
}

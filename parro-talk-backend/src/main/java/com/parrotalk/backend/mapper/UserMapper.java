package com.parrotalk.backend.mapper;

import com.parrotalk.backend.dto.UserResponse;
import com.parrotalk.backend.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}

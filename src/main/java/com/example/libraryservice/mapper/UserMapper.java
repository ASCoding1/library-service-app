package com.example.libraryservice.mapper;

import com.example.libraryservice.user.model.User;
import com.example.libraryservice.user.model.UserDto;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserDto mapToDto(User user);

}

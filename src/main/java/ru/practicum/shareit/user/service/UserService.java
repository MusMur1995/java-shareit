package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers();

    UserDto saveUser(UserDto userDto);

    User getUserById(Long userId);

    UserDto updateUser(Long userId, UserDto userDto);

    UserDto getAboutUser(Long id);

    void deleteUser(Long userId);
}

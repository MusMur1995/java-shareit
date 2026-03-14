package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private void validateEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
        if (!email.contains("@")) {
            throw new ValidationException("Некорректный формат email");
        }
    }

    @Override
    public List<UserDto> getAllUsers() {
        return UserMapper.toDto(userRepository.findAll());
    }

    @Override
    public UserDto saveUser(UserDto userDto) {
        validateEmailFormat(userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Email уже существует");
        }

        User user = UserMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        return UserMapper.toDto(savedUser);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с id: " + userId));
    }


    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = getUserById(userId);

        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            existingUser.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
            validateEmailFormat(userDto.getEmail());

            userRepository.findByEmail(userDto.getEmail())
                    .ifPresent(user -> {
                        if (!user.getId().equals(userId)) {
                            throw new ConflictException("Email уже существует");
                        }
                    });

            existingUser.setEmail(userDto.getEmail());
        }

        User updatedUser = userRepository.save(existingUser);
        return UserMapper.toDto(updatedUser);
    }

    @Override
    public UserDto getAboutUser(Long id) {
        User user = getUserById(id);
        return UserMapper.toDto(user);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден с id: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
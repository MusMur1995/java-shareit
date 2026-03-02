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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с id: " + userId));
    }

    private void validateEmailFormat(String email) {
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
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }

        validateEmailFormat(userDto.getEmail());

        boolean emailExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getEmail().equals(userDto.getEmail()));
        if (emailExists) {
            throw new ConflictException("Эмейл уже существует");
        }

        User user = UserMapper.toEntity(userDto);
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = findUserById(userId);
        return UserMapper.toDto(user);
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = findUserById(userId);

        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            existingUser.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {

            validateEmailFormat(userDto.getEmail());

            Optional<User> userWithSameEmail = userRepository.findByEmail(userDto.getEmail());
            if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(userId)) {
                throw new ConflictException("Эмейл уже существует");
            }

            existingUser.setEmail(userDto.getEmail());
        }

        return UserMapper.toDto(existingUser);
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден с id: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
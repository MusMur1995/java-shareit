package ru.practicum.shareit.user.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final List<User> users = new ArrayList<>();
    private long nextId = 1;

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public User save(User user) {
        user.setId(nextId++);
        users.add(user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public void deleteById(Long id) {
        users.removeIf(user -> user.getId().equals(id));
    }

    @Override
    public boolean existsById(Long id) {
        return users.stream().anyMatch(user -> user.getId().equals(id));
    }
}
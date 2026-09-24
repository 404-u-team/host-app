package dev.hostapp.backend.service;

import dev.hostapp.backend.exception.UserNotFoundException;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository userRepository) {
        this.repository = userRepository;
    }

    public User getByEmail(String email) {
        final Optional<User> user = repository.findByEmail(email);
        if (!user.isPresent()) {
            throw new UserNotFoundException("User not found");
        }
        return user.get();
    }

    public User createUser(String name, String surname, String email, String passwordHash) {
        User user = new User(name, surname, email, passwordHash);

        return repository.save(user);
    }

    public List<User> getAll() {
        return repository.findAll();
    }
}

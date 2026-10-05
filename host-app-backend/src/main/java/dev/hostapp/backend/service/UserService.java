package dev.hostapp.backend.service;

import dev.hostapp.backend.exceptions.UserNotFoundException;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository userRepository) {
        this.repository = userRepository;
    }

    public User getByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public boolean emailExists(String email) {
        return repository.findByEmail(email).isPresent();
    }

    public User createUser(String name, String surname, String email, String passwordHash) {
        User user = new User(name, surname, email, passwordHash);

        return repository.save(user);
    }
}

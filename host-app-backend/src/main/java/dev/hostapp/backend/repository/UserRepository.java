package dev.hostapp.backend.repository;

import java.util.Optional;
import java.util.UUID;

import dev.hostapp.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}

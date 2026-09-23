package dev.hostapp.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.hostapp.backend.model.Session;
import dev.hostapp.backend.model.User;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findAllByUser(User user);

    Optional<Session> findByTokenHash(String token);

    int countByUser(User user);

    void deleteAllByUser(User user);
    
}

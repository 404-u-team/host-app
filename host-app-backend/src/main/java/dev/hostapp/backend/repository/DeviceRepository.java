package dev.hostapp.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.hostapp.backend.model.Device;
import dev.hostapp.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findAllByUser(User user);

    Optional<Device> findByToken(String token);

    int countByUser(User user);

    void deleteAllByUser(User user);
}

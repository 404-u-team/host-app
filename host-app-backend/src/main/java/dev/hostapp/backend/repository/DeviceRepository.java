package dev.hostapp.backend.repository;

import java.util.Optional;
import java.util.UUID;

import dev.hostapp.backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByTokenHash(String token);
}

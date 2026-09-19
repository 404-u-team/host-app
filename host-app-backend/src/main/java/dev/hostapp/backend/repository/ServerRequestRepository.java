package dev.hostapp.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.hostapp.backend.model.ServerRequest;


public interface ServerRequestRepository extends JpaRepository<ServerRequest, UUID> {
    // Базовые геттеры по значению полей
    List<ServerRequest> findByOwnerLogin(String login);
    List<ServerRequest> findByOwnerId(UUID id);
    List<ServerRequest> findByCpuCores(Integer min, Integer max);
    List<ServerRequest> findByRam(Integer min, Integer max);
    List<ServerRequest> findByDiskSpace(Integer min, Integer max);
    List<ServerRequest> findByStatus(ServerRequest.RequestStatus targetStatus);

    // Счетчики по значению полей
    Integer countByOwnerLogin(String login);
    Integer countByOwnerId(UUID id);
    Integer countByCpuCores(Integer min, Integer max);
    Integer countByRam(Integer min, Integer max);
    Integer countByDiskSpace(Integer min, Integer max);
    Integer countByStatus(Integer min, Integer max);

    // Очистка
    void deleteAllByOwnerLogin(String login);
    void deleteAllByOwnerId(UUID id);
}

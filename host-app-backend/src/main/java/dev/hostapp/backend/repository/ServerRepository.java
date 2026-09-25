package dev.hostapp.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.hostapp.backend.model.Server;

public interface ServerRepository extends JpaRepository<Server, UUID> {

    // === Базовые поиски ===
    Optional<Server> findByHostname(String hostname);

    List<Server> findAllByStatus(Server.ServerStatus status);

    List<Server> findAllByStatusIn(List<Server.ServerStatus> statuses);

    // === Поиск по истории запросов ===
    List<Server> findAllByRequestIdsHistoryContains(UUID requestId);

    // === Поиск по IP-адресам ===
    List<Server> findAllByIpv4AddressesContains(String ipv4);

    List<Server> findAllByIpv6AddressesContains(String ipv6);

    // === Сортировка и пагинация ===
    Page<Server> findAllByStatus(Server.ServerStatus status, Pageable pageable);

    Page<Server> findAllByStatusIn(List<Server.ServerStatus> statuses, Pageable pageable);

    Page<Server> findAll(Pageable pageable);

    // === Статистика ===
    long countByStatus(Server.ServerStatus status);

    @Query("SELECT COUNT(s) FROM Server s WHERE s.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<Server.ServerStatus> statuses);

    @Query("SELECT COUNT(s) FROM Server s WHERE :requestId MEMBER OF s.requestIdsHistory")
    long countByRequestIdInHistory(@Param("requestId") UUID requestId);

    // === Массовые операции ===
    void deleteAllByStatus(Server.ServerStatus status);

    @Query("UPDATE Server s SET s.status = :status WHERE s.id = :id")
    int updateStatusById(@Param("status") Server.ServerStatus status, @Param("id") UUID id);

    @Query("UPDATE Server s SET s.hostname = :hostname WHERE s.id = :id")
    int updateHostnameById(@Param("hostname") String hostname, @Param("id") UUID id);

    // === Проверки существования ===
    boolean existsByHostname(String hostname);

    boolean existsByIdAndStatus(UUID id, Server.ServerStatus status);
}
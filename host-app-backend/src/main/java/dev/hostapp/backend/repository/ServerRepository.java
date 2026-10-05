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

    // === Поиск по IP-адресам ===
    List<Server> findAllByIpv4AddressesContains(String ipv4);

    List<Server> findAllByIpv6AddressesContains(String ipv6);

    Page<Server> findAll(Pageable pageable);

    // === Массовые операции ===

    @Query("UPDATE Server s SET s.hostname = :hostname WHERE s.id = :id")
    int updateHostnameById(@Param("hostname") String hostname, @Param("id") UUID id);

    // === Проверки существования ===
    boolean existsByHostname(String hostname);
}

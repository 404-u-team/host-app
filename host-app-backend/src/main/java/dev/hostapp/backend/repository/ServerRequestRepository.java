package dev.hostapp.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;

public interface ServerRequestRepository extends JpaRepository<ServerRequest, UUID> {

    // === Базовые поиски (по User объекту) ===
    List<ServerRequest> findAllByOwner(User owner);

    List<ServerRequest> findAllByStatus(ServerRequest.RequestStatus status);

    List<ServerRequest> findAllByOwnerAndStatus(User owner, ServerRequest.RequestStatus status);

    // === Базовые поиски (по ownerId — UUID) ===
    List<ServerRequest> findAllByOwnerId(UUID ownerId);

    List<ServerRequest> findAllByOwnerIdAndStatus(UUID ownerId, ServerRequest.RequestStatus status);

    // === Базовые поиски (по email владельца) ===
    List<ServerRequest> findAllByOwnerEmail(String email);

    List<ServerRequest> findAllByOwnerEmailAndStatus(String email, ServerRequest.RequestStatus status);

    // === Поиск по строковым полям ===
    List<ServerRequest> findAllByOsContainingIgnoreCase(String os);

    List<ServerRequest> findAllByCpuCoresGreaterThanEqual(Integer minCpuCores);

    // === Фильтры по диапазонам (включительные: min..max, если min==max — точное совпадение) ===
    List<ServerRequest> findAllByCpuCoresBetween(Integer minCpuCores, Integer maxCpuCores);

    List<ServerRequest> findAllByRamGbBetween(Integer minRamGb, Integer maxRamGb);

    List<ServerRequest> findAllByDiskGbBetween(Integer minDiskGb, Integer maxDiskGb);

    List<ServerRequest> findAllByCreatedAtBetween(Instant start, Instant end);

    // === Сортировка и пагинация (по User) ===
    Page<ServerRequest> findAllByOwner(User owner, Pageable pageable);

    Page<ServerRequest> findAllByStatus(ServerRequest.RequestStatus status, Pageable pageable);

    Page<ServerRequest> findAllByOwnerAndStatus(User owner, ServerRequest.RequestStatus status, Pageable pageable);

    // === Сортировка и пагинация (по ownerId) ===
    Page<ServerRequest> findAllByOwnerId(UUID ownerId, Pageable pageable);

    Page<ServerRequest> findAllByOwnerIdAndStatus(UUID ownerId, ServerRequest.RequestStatus status, Pageable pageable);

    // === Сортировка и пагинация (по email) ===
    Page<ServerRequest> findAllByOwnerEmail(String email, Pageable pageable);

    Page<ServerRequest> findAllByOwnerEmailAndStatus(String email, ServerRequest.RequestStatus status, Pageable pageable);

    Page<ServerRequest> findAll(Pageable pageable);

    // === Статистика (по User) ===
    long countByOwner(User owner);

    long countByStatus(ServerRequest.RequestStatus status);

    @Query("SELECT COUNT(r) FROM ServerRequest r WHERE r.owner = :owner AND r.status = :status")
    long countByOwnerAndStatus(@Param("owner") User owner, @Param("status") ServerRequest.RequestStatus status);

    @Query("SELECT AVG(r.cpuCores) FROM ServerRequest r WHERE r.owner = :owner")
    Double getAverageCpuCoresByOwner(@Param("owner") User owner);

    @Query("SELECT AVG(r.ramGb) FROM ServerRequest r WHERE r.owner = :owner")
    Double getAverageRamGbByOwner(@Param("owner") User owner);

    @Query("SELECT AVG(r.diskGb) FROM ServerRequest r WHERE r.owner = :owner")
    Double getAverageDiskGbByOwner(@Param("owner") User owner);

    @Query("SELECT MAX(r.cpuCores) FROM ServerRequest r WHERE r.owner = :owner")
    Integer getMaxCpuCoresByOwner(@Param("owner") User owner);

    @Query("SELECT MIN(r.cpuCores) FROM ServerRequest r WHERE r.owner = :owner")
    Integer getMinCpuCoresByOwner(@Param("owner") User owner);

    @Query("SELECT MAX(r.ramGb) FROM ServerRequest r WHERE r.owner = :owner")
    Integer getMaxRamGbByOwner(@Param("owner") User owner);

    @Query("SELECT MIN(r.ramGb) FROM ServerRequest r WHERE r.owner = :owner")
    Integer getMinRamGbByOwner(@Param("owner") User owner);

    @Query("SELECT MAX(r.diskGb) FROM ServerRequest r WHERE r.owner = :owner")
    Integer getMaxDiskGbByOwner(@Param("owner") User owner);

    @Query("SELECT MIN(r.diskGb) FROM ServerRequest r WHERE r.owner = :owner")
    Integer getMinDiskGbByOwner(@Param("owner") User owner);

    // === Статистика (по ownerId) ===
    long countByOwnerId(UUID ownerId);

    @Query("SELECT COUNT(r) FROM ServerRequest r WHERE r.owner.id = :ownerId AND r.status = :status")
    long countByOwnerIdAndStatus(@Param("ownerId") UUID ownerId, @Param("status") ServerRequest.RequestStatus status);

    @Query("SELECT AVG(r.cpuCores) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Double getAverageCpuCoresByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT AVG(r.ramGb) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Double getAverageRamGbByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT AVG(r.diskGb) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Double getAverageDiskGbByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT MAX(r.cpuCores) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Integer getMaxCpuCoresByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT MIN(r.cpuCores) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Integer getMinCpuCoresByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT MAX(r.ramGb) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Integer getMaxRamGbByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT MIN(r.ramGb) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Integer getMinRamGbByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT MAX(r.diskGb) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Integer getMaxDiskGbByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT MIN(r.diskGb) FROM ServerRequest r WHERE r.owner.id = :ownerId")
    Integer getMinDiskGbByOwnerId(@Param("ownerId") UUID ownerId);

    // === Статистика (по email) ===
    long countByOwnerEmail(String email);

    @Query("SELECT COUNT(r) FROM ServerRequest r WHERE r.owner.email = :email AND r.status = :status")
    long countByOwnerEmailAndStatus(@Param("email") String email, @Param("status") ServerRequest.RequestStatus status);

    // === Массовые операции (по User) ===
    void deleteAllByOwner(User owner);

    @Query("UPDATE ServerRequest r SET r.status = :status WHERE r.owner = :owner AND r.id = :id")
    int updateStatusByOwnerAndId(@Param("status") ServerRequest.RequestStatus status,
                                  @Param("owner") User owner,
                                  @Param("id") UUID id);

    // === Массовые операции (по ownerId) ===
    void deleteAllByOwnerId(UUID ownerId);

    @Query("UPDATE ServerRequest r SET r.status = :status WHERE r.owner.id = :ownerId AND r.id = :id")
    int updateStatusByOwnerIdAndId(@Param("status") ServerRequest.RequestStatus status,
                                    @Param("ownerId") UUID ownerId,
                                    @Param("id") UUID id);

    // === Массовые операции (по email) ===
    void deleteAllByOwnerEmail(String email);

    @Query("UPDATE ServerRequest r SET r.status = :status WHERE r.owner.email = :email AND r.id = :id")
    int updateStatusByOwnerEmailAndId(@Param("status") ServerRequest.RequestStatus status,
                                       @Param("email") String email,
                                       @Param("id") UUID id);

    // === Проверки существования (по User) ===
    boolean existsByOwnerAndId(User owner, UUID id);

    Optional<ServerRequest> findByOwnerAndId(User owner, UUID id);

    // === Проверки существования (по ownerId) ===
    boolean existsByOwnerIdAndId(UUID ownerId, UUID id);

    Optional<ServerRequest> findByOwnerIdAndId(UUID ownerId, UUID id);

    // === Проверки существования (по email) ===
    boolean existsByOwnerEmailAndId(String email, UUID id);

    Optional<ServerRequest> findByOwnerEmailAndId(String email, UUID id);
}

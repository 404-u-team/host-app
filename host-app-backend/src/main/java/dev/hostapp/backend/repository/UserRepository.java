package dev.hostapp.backend.repository;

import java.util.Optional;
import java.util.UUID;

import dev.hostapp.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByEmailIgnoreCaseOrderByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findTopByEmailIgnoreCaseOrderByEmailAsc(String email);

    default Optional<User> lockLogin(String email) {
        return findByEmailForUpdate(email).or(() -> findTopByEmailIgnoreCaseOrderByEmailAsc(email));
    }
}

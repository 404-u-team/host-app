package dev.hostapp.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Database default preserves access for accounts created before email verification.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified = true;

    private String emailCodeHash;
    private Instant emailCodeExpiresAt;
    private Instant emailCodeSentAt;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int emailCodeAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String name, String surname, String email, String passwordHash) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void requireEmailVerification() {
        emailVerified = false;
    }

    public void issueEmailCode(String hash, Instant now) {
        emailCodeHash = hash;
        emailCodeSentAt = now;
        emailCodeExpiresAt = now.plusSeconds(600);
        emailCodeAttempts = 0;
    }

    public String getEmailCodeHash() { return emailCodeHash; }
    public Instant getEmailCodeExpiresAt() { return emailCodeExpiresAt; }
    public Instant getEmailCodeSentAt() { return emailCodeSentAt; }
    public int getEmailCodeAttempts() { return emailCodeAttempts; }
    public void recordEmailCodeAttempt() { emailCodeAttempts++; }

    public void verifyEmail() {
        emailVerified = true;
        emailCodeHash = null;
        emailCodeExpiresAt = null;
        emailCodeSentAt = null;
        emailCodeAttempts = 0;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

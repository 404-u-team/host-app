package dev.hostapp.backend.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

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

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean mustChangePassword;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long authVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

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
        roles.add("ADMIN".equalsIgnoreCase(email) ? Role.ADMIN : Role.EDITOR);
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

    public Set<Role> getRoles() {
        if (roles.isEmpty()) {
            return Set.of("ADMIN".equalsIgnoreCase(email) ? Role.ADMIN : Role.EDITOR);
        }
        return Set.copyOf(roles);
    }

    public boolean isAdmin() { return getRoles().contains(Role.ADMIN); }
    public boolean canEdit() { return isAdmin() || getRoles().contains(Role.EDITOR); }
    public boolean isActive() { return active; }
    public boolean mustChangePassword() { return mustChangePassword; }
    public long getAuthVersion() { return authVersion; }

    public void updateAccount(String name, String surname, String email, Set<Role> roles, boolean active) {
        if (!this.email.equals(email) || this.active != active || !getRoles().equals(roles)) {
            authVersion++;
        }
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.active = active;
        this.roles.clear();
        this.roles.addAll(roles);
    }

    public void setPassword(String hash, boolean temporary) {
        passwordHash = hash;
        mustChangePassword = temporary;
        authVersion++;
    }

    public void upgradePasswordHash(String hash) { passwordHash = hash; }

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

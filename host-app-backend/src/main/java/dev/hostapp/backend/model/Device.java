package dev.hostapp.backend.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn (name = "user_id", nullable = false)
    private User user;

    @Column
    private String ip;

    @Column (name = "user_agent")
    private String userAgent;

    @Column (nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant sessionValidUntil;

    @Column(nullable = false, updatable = false)
    private Instant sessionCreatedAt;

    @Column(nullable = false)
    private Instant sessionLastUsedAt;

    protected Device(){};

    public Device(User user, String ip, String userAgent, String token) {
        this.user = user;
        this.ip = ip;
        this.userAgent = userAgent;
        this.tokenHash = token;
    }

    @PrePersist
    private void onCreate() {
        sessionCreatedAt = Instant.now();
        sessionLastUsedAt = Instant.now();
        sessionValidUntil = Instant.now().plus(7, ChronoUnit.DAYS);
    }

    @PreUpdate
    private void onUpdate() {
        sessionLastUsedAt = Instant.now();
    }

    public Instant getSessionValidUntil() {
        return sessionValidUntil;
    }

    public void revalidateSession() {
        sessionLastUsedAt = Instant.now();
        sessionValidUntil = Instant.now().plus(7, ChronoUnit.DAYS);
    }


}

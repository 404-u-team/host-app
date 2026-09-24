package dev.hostapp.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "servers")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ElementCollection
    @Column(name = "request_id")
    private List<UUID> requestIdsHistory = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String hostname;

    @ElementCollection
    @Column(name = "ipv4_address")
    private List<String> ipv4Addresses = new ArrayList<>();

    @ElementCollection
    @Column(name = "ipv6_address")
    private List<String> ipv6Addresses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerStatus status = ServerStatus.OFF;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Server() {
    }

    public Server(String hostname) {
        this.hostname = hostname;
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

    public List<UUID> getRequestIdsHistory() {
        return requestIdsHistory;
    }

    public void setRequestIdsHistory(List<UUID> requestIdsHistory) {
        this.requestIdsHistory = requestIdsHistory != null ? requestIdsHistory : new ArrayList<>();
    }

    public void addRequestToHistory(UUID requestId) {
        if (requestId != null) {
            this.requestIdsHistory.add(requestId);
        }
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public List<String> getIpv4Addresses() {
        return ipv4Addresses;
    }

    public void setIpv4Addresses(List<String> ipv4Addresses) {
        this.ipv4Addresses = ipv4Addresses != null ? ipv4Addresses : new ArrayList<>();
    }

    public void addIpv4Address(String ip) {
        if (ip != null) {
            this.ipv4Addresses.add(ip);
        }
    }

    public List<String> getIpv6Addresses() {
        return ipv6Addresses;
    }

    public void setIpv6Addresses(List<String> ipv6Addresses) {
        this.ipv6Addresses = ipv6Addresses != null ? ipv6Addresses : new ArrayList<>();
    }

    public void addIpv6Address(String ip) {
        if (ip != null) {
            this.ipv6Addresses.add(ip);
        }
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum ServerStatus {
        OFF,
        ON,
        SUSPENDED
    }
}

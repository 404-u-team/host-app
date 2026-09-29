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
import jakarta.persistence.Version;

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

    // === Ресурсы сервера (физические) ===
    @Column(name = "cpu_cores", nullable = false)
    private Integer cpuCores;

    @Column(name = "ram_gb", nullable = false)
    private Integer ramGb;

    @Column(name = "disk_gb", nullable = false)
    private Integer diskGb;

    // === Доступные (свободные) ресурсы для бронирования ===
    @Column(name = "available_cpu_cores", nullable = false)
    private Integer availableCpuCores;

    @Column(name = "available_ram_gb", nullable = false)
    private Integer availableRamGb;

    @Column(name = "available_disk_gb", nullable = false)
    private Integer availableDiskGb;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Server() {
    }

    public Server(String hostname, Integer cpuCores, Integer ramGb, Integer diskGb) {
        this.hostname = hostname;
        this.cpuCores = cpuCores;
        this.ramGb = ramGb;
        this.diskGb = diskGb;
        // Изначально все ресурсы свободны
        this.availableCpuCores = cpuCores;
        this.availableRamGb = ramGb;
        this.availableDiskGb = diskGb;
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

    // === Логика бронирования ресурсов ===
    public boolean canAllocate(Integer cpuCores, Integer ramGb, Integer diskGb) {
        return availableCpuCores >= cpuCores
                && availableRamGb >= ramGb
                && availableDiskGb >= diskGb;
    }

    public void allocate(Integer cpuCores, Integer ramGb, Integer diskGb) {
        if (!canAllocate(cpuCores, ramGb, diskGb)) {
            throw new IllegalStateException("Недостаточно ресурсов на сервере " + hostname);
        }
        this.availableCpuCores -= cpuCores;
        this.availableRamGb -= ramGb;
        this.availableDiskGb -= diskGb;
    }

    public void release(Integer cpuCores, Integer ramGb, Integer diskGb) {
        this.availableCpuCores += cpuCores;
        this.availableRamGb += ramGb;
        this.availableDiskGb += diskGb;
        // Не превышаем total
        this.availableCpuCores = Math.min(this.availableCpuCores, this.cpuCores);
        this.availableRamGb = Math.min(this.availableRamGb, this.ramGb);
        this.availableDiskGb = Math.min(this.availableDiskGb, this.diskGb);
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
        if (requestId != null)
            this.requestIdsHistory.add(requestId);
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
        if (ip != null)
            this.ipv4Addresses.add(ip);
    }

    public List<String> getIpv6Addresses() {
        return ipv6Addresses;
    }

    public void setIpv6Addresses(List<String> ipv6Addresses) {
        this.ipv6Addresses = ipv6Addresses != null ? ipv6Addresses : new ArrayList<>();
    }

    public void addIpv6Address(String ip) {
        if (ip != null)
            this.ipv6Addresses.add(ip);
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getRamGb() {
        return ramGb;
    }

    public void setRamGb(Integer ramGb) {
        this.ramGb = ramGb;
    }

    public Integer getDiskGb() {
        return diskGb;
    }

    public void setDiskGb(Integer diskGb) {
        this.diskGb = diskGb;
    }

    public Integer getAvailableCpuCores() {
        return availableCpuCores;
    }

    public Integer getAvailableRamGb() {
        return availableRamGb;
    }

    public Integer getAvailableDiskGb() {
        return availableDiskGb;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum ServerStatus {
        OFF, ON, SUSPENDED
    }
}
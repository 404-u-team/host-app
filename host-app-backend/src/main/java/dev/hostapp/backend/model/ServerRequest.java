package dev.hostapp.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "server_requests")
public class ServerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "cpu_cores", nullable = false)
    private Integer cpuCores;

    @Column(name = "ram_gb", nullable = false)
    private Integer ramGb;

    @Column(name = "disk_gb", nullable = false)
    private Integer diskGb;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OSType os;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServerRequest() {
    }

    public ServerRequest(User owner, Integer cpuCores, Integer ramGb, Integer diskGb, OSType os) {
        this.owner = owner;
        this.cpuCores = cpuCores;
        this.ramGb = ramGb;
        this.diskGb = diskGb;
        this.os = os;
        this.status = RequestStatus.CREATED;
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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

    public OSType getOs() {
        return os;
    }

    public void setOs(OSType os) {
        this.os = os;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum RequestStatus {
        CREATED,
        APPROVED,
        REJECTED,
        COMPLETED,
        CANCELLED
    }

    public enum OSType {
        WINDOWS10,
        WINDOWS11,
        ARCH,
        DEBIAN,
        CUSTOM
    }
}

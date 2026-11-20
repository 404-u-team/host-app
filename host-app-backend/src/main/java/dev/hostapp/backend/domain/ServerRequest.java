package dev.hostapp.backend.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Version;

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

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    @Column(name = "cpu_cores", nullable = false)
    private Integer cpuCores;

    @Column(name = "ram_gb", nullable = false)
    private Integer ramGb;

    @Column(name = "disk_gb", nullable = false)
    private Integer diskGb;

    @Column(nullable = false)
    private String os;

    @Column(unique = true, length = 80)
    private String externalId;

    @Column(nullable = false, length = 160, columnDefinition = "varchar(160) default 'Заявка на сервер'")
    private String title = "Заявка на сервер";

    @Column(nullable = false, length = 2000, columnDefinition = "varchar(2000) default ''")
    private String description = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_id")
    private OsReference reference;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean urgent;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean archived;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    @ElementCollection
    @CollectionTable(name = "request_tags", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "tag", nullable = false, length = 48)
    private Set<String> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) default 'STANDARD'")
    private Priority priority = Priority.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    // Existing requests remain creation requests when Hibernate adds the column.
    @Column(nullable = false, columnDefinition = "varchar(255) default 'CREATE'")
    private RequestType type = RequestType.CREATE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServerRequest() {
    }

    public ServerRequest(User owner, Integer cpuCores, Integer ramGb, Integer diskGb, String os) {
        this.owner = owner;
        this.cpuCores = cpuCores;
        this.ramGb = ramGb;
        this.diskGb = diskGb;
        this.os = os;
        this.status = RequestStatus.CREATED;
        this.externalId = "REQ-" + UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT);
    }

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        priority = getPriority();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
        priority = getPriority();
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

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
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

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getExternalId() { return externalId == null ? "REQ-" + id.toString().toUpperCase(java.util.Locale.ROOT) : externalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public OsReference getReference() { return reference; }
    public boolean isUrgent() { return urgent; }
    public boolean isArchived() { return archived; }
    public long getVersion() { return version; }
    public Set<String> getTags() { return Set.copyOf(tags); }
    public Priority getPriority() {
        return urgent || cpuCores >= 16 || ramGb >= 64 || diskGb >= 500 ? Priority.HIGH : Priority.STANDARD;
    }

    public void setReference(OsReference reference) { this.reference = reference; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void addTag(String tag) { tags.add(tag); updatedAt = Instant.now(); }
    public void removeTag(String tag) { tags.remove(tag); updatedAt = Instant.now(); }

    public void setDetails(String externalId, String title, String description, boolean urgent, Set<String> tags) {
        this.externalId = externalId;
        this.title = title;
        this.description = description;
        this.urgent = urgent;
        this.tags.clear();
        this.tags.addAll(tags);
        updatedAt = Instant.now();
    }

    public enum Priority { STANDARD, HIGH }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum RequestType {
        CREATE,
        CHANGE,
        CANCEL
    }

    public enum RequestStatus {
        CREATED,
        APPROVED,
        REJECTED,
        COMPLETED,
        CANCELLED
    }
}

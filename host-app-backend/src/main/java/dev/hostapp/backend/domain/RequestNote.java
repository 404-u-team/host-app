package dev.hostapp.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "request_notes")
public class RequestNote {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ServerRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RequestNote() {}

    public RequestNote(ServerRequest request, User author, String body, boolean pinned) {
        this.request = request;
        this.author = author;
        this.createdAt = Instant.now();
        update(body, pinned);
    }

    public void update(String body, boolean pinned) {
        this.body = body;
        this.pinned = pinned;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAuthorId() { return author.getId(); }
    public String getBody() { return body; }
    public boolean isPinned() { return pinned; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package com.cpsync.cpsync_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "synced_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "contest_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contest_key", nullable = false)
    private String contestKey;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    protected void onCreate() {
        syncedAt = LocalDateTime.now();
    }
}
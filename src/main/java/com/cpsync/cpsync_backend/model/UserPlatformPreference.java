package com.cpsync.cpsync_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_platform_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "platform"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPlatformPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(nullable = false)
    private boolean enabled = true;
}
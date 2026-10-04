package com.example.short_video.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "watch_histories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_watch_history_user_video", columnNames = {"user_id", "video_id"})
}, indexes = {
        @Index(name = "idx_watch_history_user_last_watched", columnList = "user_id, last_watched_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "watched_duration")
    private Double watchedDuration;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "watch_count", nullable = false)
    @Builder.Default
    private Integer watchCount = 1;

    @Column(name = "last_watched_at", nullable = false)
    private Instant lastWatchedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

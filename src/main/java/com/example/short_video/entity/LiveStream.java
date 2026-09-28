package com.example.short_video.entity;

import com.example.short_video.Enum.LiveStreamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "live_streams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LiveStreamStatus status = LiveStreamStatus.LIVE;
    @Column(name = "room_id", nullable = false, unique = true, length = 100)
    private String roomId;
    @Column(name = "stream_uid", length = 100)
    private String streamUid;
    @Column(name = "viewer_count")
    @Builder.Default
    private Integer viewerCount = 0;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;
    public String getEffectiveRoomId() {
        return roomId != null ? roomId : streamUid;
    }
}

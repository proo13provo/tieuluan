package com.example.short_video.repository;

import com.example.short_video.Enum.LiveStreamStatus;
import com.example.short_video.entity.LiveStream;
import com.example.short_video.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

    Optional<LiveStream> findByStreamUid(String streamUid);

    List<LiveStream> findByUserOrderByStartedAtDesc(User user);

    List<LiveStream> findByStatus(LiveStreamStatus status);

    List<LiveStream> findByStatusOrderByStartedAtDesc(LiveStreamStatus status);

    Optional<LiveStream> findFirstByUserAndStatus(User user, LiveStreamStatus status);
}

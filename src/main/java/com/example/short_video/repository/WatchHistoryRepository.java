package com.example.short_video.repository;

import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    Optional<WatchHistory> findByUserAndVideo(User user, Video video);

    Optional<WatchHistory> findByUserAndVideoId(User user, Long videoId);

    @Query(value = "SELECT w FROM WatchHistory w JOIN FETCH w.video v LEFT JOIN FETCH v.user WHERE w.user = :user ORDER BY w.lastWatchedAt DESC",
           countQuery = "SELECT COUNT(w) FROM WatchHistory w WHERE w.user = :user")
    Page<WatchHistory> findByUserOrderByLastWatchedAtDesc(@Param("user") User user, Pageable pageable);

    @Modifying
    @Query("DELETE FROM WatchHistory w WHERE w.user = :user AND w.video.id = :videoId")
    int deleteByUserAndVideoId(@Param("user") User user, @Param("videoId") Long videoId);

    @Modifying
    @Query("DELETE FROM WatchHistory w WHERE w.user = :user")
    int deleteAllByUser(@Param("user") User user);

    boolean existsByUserAndVideo(User user, Video video);

    long countByUser(User user);
}

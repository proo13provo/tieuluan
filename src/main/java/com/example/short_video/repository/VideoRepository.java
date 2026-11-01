package com.example.short_video.repository;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserOrderByCreatedAtDesc(User user);
    List<Video> findByUserAndStatusOrderByCreatedAtDesc(User user, VideoStatus status);

    List<Video> findByStatus(VideoStatus status);
    List<Video> findByStatusOrderByCreatedAtDesc(VideoStatus status);
    List<Video> findByStatusOrderByCreatedAtDesc(VideoStatus status, Pageable pageable);
    Page<Video> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(VideoStatus status);

    @Query("SELECT COALESCE(SUM(v.views), 0) FROM Video v")
    long sumTotalViews();

    @Query("SELECT COALESCE(SUM(v.likes), 0) FROM Video v")
    long sumTotalLikes();

    @Query("SELECT COALESCE(SUM(v.shares), 0) FROM Video v")
    long sumTotalShares();

    List<Video> findTop10ByOrderByViewsDesc();

    @Modifying
    @Query("UPDATE Video v SET v.shares = COALESCE(v.shares, 0) + 1 WHERE v.id = :videoId")
    int incrementShares(@Param("videoId") Long videoId);

    @Query("SELECT DISTINCT v FROM Video v " +
           "LEFT JOIN v.hashtags h " +
           "LEFT JOIN v.categories c " +
           "WHERE v.status = :status AND (" +
           "LOWER(v.title) LIKE :pattern OR " +
           "LOWER(h.name) LIKE :pattern OR " +
           "LOWER(c.name) LIKE :pattern)")
    Page<Video> searchVideosFallback(
            @Param("pattern") String pattern,
            @Param("status") VideoStatus status,
            Pageable pageable
    );

}

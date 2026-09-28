package com.example.short_video.repository;

import com.example.short_video.entity.Like;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    @Query("SELECT COUNT(l) FROM Like l WHERE l.video.user = :user")
    long countTotalLikesByVideoOwner(@Param("user") User user);

    Optional<Like> findByUserAndVideo(User user, Video video);
    boolean existsByVideoAndUser(Video video, User user);
    long countByVideo(Video video);
    void deleteByVideoAndUser(Video video, User user);
}

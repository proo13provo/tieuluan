package com.example.short_video.repository;

import com.example.short_video.entity.Comment;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByVideoAndParentCommentIsNullOrderByCreatedAtDesc(Video video, Pageable pageable);
    List<Comment> findByParentCommentOrderByCreatedAtAsc(Comment parentComment);
    long countByVideo(Video video);
    long countByParentComment(Comment parentComment);

    @Query("SELECT DISTINCT c.video.id FROM Comment c WHERE c.user = :user")
    java.util.List<Long> findCommentedVideoIdsByUser(@Param("user") User user);
}

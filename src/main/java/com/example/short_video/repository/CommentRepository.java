package com.example.short_video.repository;

import com.example.short_video.entity.Comment;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByVideoAndParentCommentIsNullOrderByCreatedAtDesc(Video video, Pageable pageable);
    List<Comment> findByParentCommentOrderByCreatedAtAsc(Comment parentComment);
    long countByVideo(Video video);
    long countByParentComment(Comment parentComment);
}

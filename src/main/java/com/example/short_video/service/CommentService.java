package com.example.short_video.service;

import com.example.short_video.dto.request.CommentRequest;
import com.example.short_video.dto.response.CommentResponse;
import com.example.short_video.entity.Comment;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.repository.CommentRepository;
import com.example.short_video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    /**
     * Thêm comment hoặc reply vào video.
     * Nếu request.parentCommentId != null → đây là reply.
     */
    @Transactional
    public CommentResponse addComment(Long videoId, CommentRequest request, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        if (!video.isIscomment()) { // Lombok generates isIscomment() for boolean field 'iscomment'
            throw new IllegalStateException("Video này đã tắt tính năng bình luận");
        }

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy comment với id: " + request.getParentCommentId()));
            // Reply chỉ được phép ở cấp 1 (reply của comment gốc, không reply của reply)
            if (parentComment.getParentComment() != null) {
                parentComment = parentComment.getParentComment();
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(user)
                .video(video)
                .parentComment(parentComment)
                .build();

        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    /**
     * Lấy danh sách comment gốc của video (phân trang).
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long videoId, int page, int size) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        Pageable pageable = PageRequest.of(page, size);
        return commentRepository
                .findByVideoAndParentCommentIsNullOrderByCreatedAtDesc(video, pageable)
                .map(this::toResponse);
    }

    /**
     * Lấy tất cả reply của một comment.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(Long commentId) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy comment với id: " + commentId));

        return commentRepository.findByParentCommentOrderByCreatedAtAsc(parent)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Xóa comment.
     * Chỉ cho phép: tác giả comment hoặc chủ video.
     */
    @Transactional
    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy comment với id: " + commentId));

        boolean isAuthor = comment.getUser().getId().equals(user.getId());
        boolean isVideoOwner = comment.getVideo().getUser().getId().equals(user.getId());

        if (!isAuthor && !isVideoOwner) {
            throw new IllegalArgumentException("Bạn không có quyền xóa bình luận này");
        }

        commentRepository.delete(comment);
    }

    // ─── Mapper helper ───────────────────────────────────────────────────────

    private CommentResponse toResponse(Comment comment) {
        long replyCount = comment.getParentComment() == null
                ? commentRepository.countByParentComment(comment)
                : 0L;

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .avatarUrl(comment.getUser().getAvatarUrl())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .replyCount(replyCount)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

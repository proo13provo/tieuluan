package com.example.short_video.service;


import com.example.short_video.dto.response.LikeResponse;
import com.example.short_video.entity.Like;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.repository.LikeRepository;
import com.example.short_video.repository.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final VideoRepository videoRepository;
    private final LikeRepository likeRepository;

    @Transactional
    public LikeResponse toggleLike(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        boolean alreadyLiked = likeRepository.existsByVideoAndUser(video, user);

        if (alreadyLiked) {
            likeRepository.deleteByVideoAndUser(video, user);
            video.setLikes(Math.max(0, video.getLikes() - 1));
        } else {
            Like like = Like.builder()
                    .user(user)
                    .video(video)
                    .build();
            likeRepository.save(like);
            video.setLikes(video.getLikes() + 1);
        }

        videoRepository.save(video);

        return LikeResponse.builder()
                .liked(!alreadyLiked)
                .totalLikes(video.getLikes())
                .build();
    }

    @Transactional
    public LikeResponse getLikeStatus(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy video với id: " + videoId));

        boolean liked = likeRepository.existsByVideoAndUser(video, user);

        return LikeResponse.builder()
                .liked(liked)
                .totalLikes(video.getLikes())
                .build();
    }
}

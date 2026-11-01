package com.example.short_video.service;

import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.request.AiRecommendRequest;
import com.example.short_video.dto.request.AiSearchRequest;
import com.example.short_video.dto.response.AiSearchResponse;
import com.example.short_video.dto.response.UnifiedSearchResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.entity.Category;
import com.example.short_video.entity.Hashtag;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.mapper.VideoMapper;
import com.example.short_video.repository.FollowRepository;
import com.example.short_video.repository.LikeRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final LikeRepository likeRepository;
    private final VideoMapper videoMapper;
    private final AiRecommendationClient aiRecommendationClient;
    @Transactional(readOnly = true)
    public UnifiedSearchResponse search(
            String query,
            int page,
            int size,
            String sortBy,
            User currentUser
    ) {
        int targetPage = Math.max(0, page);
        int targetSize = (size > 0 && size <= 50) ? size : 10;
        String cleanQuery = (query != null) ? query.trim() : "";
        List<Video> candidateVideos = videoRepository.findByStatus(VideoStatus.PUBLISHED);
        List<User> candidateUsers = userRepository.findByIsActiveTrue();

        Map<Long, Video> videoMap = (candidateVideos != null)
                ? candidateVideos.stream().collect(Collectors.toMap(Video::getId, v -> v, (v1, v2) -> v1))
                : Collections.emptyMap();

        Map<Long, User> userMap = (candidateUsers != null)
                ? candidateUsers.stream().collect(Collectors.toMap(User::getId, u -> u, (u1, u2) -> u1))
                : Collections.emptyMap();

        List<AiRecommendRequest.CandidateVideoDto> videoDtos = (candidateVideos != null)
                ? candidateVideos.stream().map(v -> {
                    List<String> catNames = v.getCategories() != null
                            ? v.getCategories().stream().map(Category::getName).toList()
                            : Collections.emptyList();
                    List<String> tagNames = v.getHashtags() != null
                            ? v.getHashtags().stream().map(Hashtag::getName).toList()
                            : Collections.emptyList();

                    return AiRecommendRequest.CandidateVideoDto.builder()
                            .id(v.getId())
                            .title(v.getTitle())
                            .views(v.getViews())
                            .likes(v.getLikes() != null ? v.getLikes() : 0L)
                            .shares(v.getShares() != null ? v.getShares() : 0L)
                            .duration(v.getDuration())
                            .createdAt(v.getCreatedAt() != null ? v.getCreatedAt().toString() : null)
                            .categories(catNames)
                            .hashtags(tagNames)
                            .build();
                }).toList()
                : Collections.emptyList();

        List<AiSearchRequest.CandidateUserDto> userDtos = (candidateUsers != null)
                ? candidateUsers.stream().map(u -> AiSearchRequest.CandidateUserDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .bio(u.getBio())
                        .avatarUrl(u.getAvatarUrl())
                        .build()).toList()
                : Collections.emptyList();

        AiSearchRequest aiSearchRequest = AiSearchRequest.builder()
                .query(cleanQuery)
                .page(targetPage)
                .limit(targetSize)
                .sortBy(sortBy != null && !sortBy.isBlank() ? sortBy.toUpperCase() : "RELEVANCE")
                .minScore(0.12)
                .candidateVideos(videoDtos)
                .candidateUsers(userDtos)
                .build();
        AiSearchResponse aiResponse = aiRecommendationClient.search(aiSearchRequest);

        if (aiResponse != null) {
            log.info("Tìm kiếm thông minh gộp chung qua AI hoàn tất: {} người dùng, {} video",
                    aiResponse.getUsers() != null ? aiResponse.getUsers().size() : 0,
                    aiResponse.getVideos() != null ? aiResponse.getVideos().size() : 0);

            Set<Long> likedVideoIds = (currentUser != null)
                    ? new HashSet<>(likeRepository.findLikedVideoIdsByUser(currentUser))
                    : Collections.emptySet();

            List<VideoResponse> mappedVideos = new ArrayList<>();
            if (aiResponse.getVideos() != null) {
                for (Map<String, Object> item : aiResponse.getVideos()) {
                    Object idObj = item.get("id");
                    if (idObj == null) continue;
                    Long vid = Long.valueOf(idObj.toString());
                    Video video = videoMap.get(vid);
                    if (video != null) {
                        VideoResponse res = videoMapper.toVideoResponse(video);
                        res.setLiked(likedVideoIds.contains(vid));

                        Object reasonObj = item.get("matchReason");
                        if (reasonObj != null) {
                            res.setMatchReason(reasonObj.toString());
                        }

                        Object scoreObj = item.get("searchScore");
                        if (scoreObj instanceof Number num) {
                            res.setSearchScore(num.doubleValue());
                        }

                        mappedVideos.add(res);
                    }
                }
            }

            List<Long> matchedUserIds = (aiResponse.getUsers() != null)
                    ? aiResponse.getUsers().stream()
                            .map(item -> item.get("id"))
                            .filter(Objects::nonNull)
                            .map(id -> Long.valueOf(id.toString()))
                            .toList()
                    : Collections.emptyList();

            Map<Long, String> reasonMap = new HashMap<>();
            if (aiResponse.getUsers() != null) {
                for (Map<String, Object> item : aiResponse.getUsers()) {
                    Object idObj = item.get("id");
                    if (idObj != null) {
                        Object reasonObj = item.get("matchReason");
                        if (reasonObj != null) {
                            reasonMap.put(Long.valueOf(idObj.toString()), reasonObj.toString());
                        }
                    }
                }
            }

            List<UserSummaryResponse> mappedUsers = mapUsersToSummaryResponses(matchedUserIds, userMap, reasonMap, currentUser);

            return UnifiedSearchResponse.builder()
                    .query(cleanQuery)
                    .totalUsers(aiResponse.getTotalUsers() > 0 ? aiResponse.getTotalUsers() : mappedUsers.size())
                    .totalVideos(aiResponse.getTotalVideos() > 0 ? aiResponse.getTotalVideos() : mappedVideos.size())
                    .users(mappedUsers)
                    .videos(mappedVideos)
                    .build();
        }

        // 4. Fallback Database nếu AI microservice offline
        log.warn("Sử dụng Fallback Database Search gộp chung cho từ khóa '{}'", cleanQuery);
        return fallbackUnifiedSearch(cleanQuery, targetPage, targetSize, currentUser);
    }

    private UnifiedSearchResponse fallbackUnifiedSearch(
            String keyword,
            int page,
            int size,
            User currentUser
    ) {
        if (keyword.isBlank()) {
            return UnifiedSearchResponse.builder()
                    .query(keyword)
                    .totalUsers(0)
                    .totalVideos(0)
                    .users(Collections.emptyList())
                    .videos(Collections.emptyList())
                    .build();
        }

        Set<Long> likedVideoIds = (currentUser != null)
                ? new HashSet<>(likeRepository.findLikedVideoIdsByUser(currentUser))
                : Collections.emptySet();

        String pattern = "%" + keyword.toLowerCase() + "%";

        Page<Video> videoPage = videoRepository.searchVideosFallback(
                pattern,
                VideoStatus.PUBLISHED,
                PageRequest.of(page, size)
        );

        List<VideoResponse> fallbackVideos = videoPage.getContent().stream().map(v -> {
            VideoResponse res = videoMapper.toVideoResponse(v);
            res.setLiked(likedVideoIds.contains(v.getId()));
            res.setMatchReason("Khớp từ khóa cơ bản (Database Fallback)");
            return res;
        }).collect(Collectors.toList());

        Page<User> userPage = userRepository.searchPublicUsersByPattern(pattern, PageRequest.of(page, size));
        List<Long> userIds = userPage.getContent().stream().map(User::getId).toList();
        Map<Long, User> userMap = userPage.getContent().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (u1, u2) -> u1));
        List<UserSummaryResponse> fallbackUsers = mapUsersToSummaryResponses(userIds, userMap, null, currentUser);

        return UnifiedSearchResponse.builder()
                .query(keyword)
                .totalUsers((int) userPage.getTotalElements())
                .totalVideos((int) videoPage.getTotalElements())
                .users(fallbackUsers)
                .videos(fallbackVideos)
                .build();
    }

    private List<UserSummaryResponse> mapUsersToSummaryResponses(
            List<Long> userIds,
            Map<Long, User> userMap,
            Map<Long, String> reasonMap,
            User currentUser
    ) {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> followingIds = (currentUser != null)
                ? followRepository.findFollowingIdsIn(currentUser, userIds)
                : Collections.emptySet();

        Set<Long> followerIds = (currentUser != null)
                ? followRepository.findFollowerIdsIn(currentUser, userIds)
                : Collections.emptySet();

        List<UserSummaryResponse> results = new ArrayList<>();
        for (Long uid : userIds) {
            User user = userMap.get(uid);
            if (user == null) continue;

            boolean isFollowing = followingIds.contains(uid);
            boolean isFollowedBy = followerIds.contains(uid);
            boolean isMutual = isFollowing && isFollowedBy;
            long followerCount = followRepository.countByFollowing(user);
            String matchReason = (reasonMap != null) ? reasonMap.get(uid) : null;

            results.add(UserSummaryResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .avatarUrl(user.getAvatarUrl())
                    .bio(user.getBio())
                    .isFollowing(isFollowing)
                    .isFollowedBy(isFollowedBy)
                    .isMutual(isMutual)
                    .followerCount(followerCount)
                    .matchReason(matchReason)
                    .build());
        }
        return results;
    }
}

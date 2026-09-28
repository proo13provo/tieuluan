package com.example.short_video.service.imp;

import com.example.short_video.Enum.LiveStreamStatus;
import com.example.short_video.config.ZegoProperties;
import com.example.short_video.dto.request.SendLiveCommentRequest;
import com.example.short_video.dto.request.StartLiveRequest;
import com.example.short_video.dto.response.LiveCommentMessage;
import com.example.short_video.dto.response.LiveStreamResponse;
import com.example.short_video.entity.LiveStream;
import com.example.short_video.entity.User;
import com.example.short_video.exception.AppException;
import com.example.short_video.repository.LiveStreamRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.service.LiveStreamService;
import com.example.short_video.service.zego.ZegoTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveStreamServiceImpl implements LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;
    private final ZegoTokenService zegoTokenService;
    private final ZegoProperties zegoProperties;
    private final SimpMessagingTemplate messagingTemplate;

    private static final long TOKEN_EFFECTIVE_SECONDS = 12 * 3600L;

    @Override
    @Transactional
    public LiveStreamResponse startLive(StartLiveRequest request, User user) {
        liveStreamRepository
                .findFirstByUserAndStatus(user, LiveStreamStatus.LIVE)
                .ifPresent(old -> {
                    old.setStatus(LiveStreamStatus.ENDED);
                    old.setEndedAt(Instant.now());
                    liveStreamRepository.save(old);
                    log.info("[Live] Auto-ended old stream {} of user {}", old.getId(), user.getUsername());
                });

        String roomId = "live_" + user.getId() + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        String title = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : user.getUsername() + " dang livestream";

        LiveStream live = LiveStream.builder()
                .user(user)
                .title(title)
                .roomId(roomId)
                .streamUid(roomId)
                .status(LiveStreamStatus.LIVE)
                .viewerCount(0)
                .build();
        live = liveStreamRepository.save(live);
        log.info("[Live] Started id={} roomId={} user={}", live.getId(), roomId, user.getUsername());

        String token = zegoTokenService.generateToken04(
                String.valueOf(user.getId()), roomId, TOKEN_EFFECTIVE_SECONDS);
        return buildResponse(live, token);
    }

    @Override
    @Transactional
    public LiveStreamResponse endLive(Long liveId, User user) {
        LiveStream live = findById(liveId);
        if (!live.getUser().getId().equals(user.getId()))
            throw new AppException(HttpStatus.FORBIDDEN, "Ban khong co quyen ket thuc stream nay");
        if (live.getStatus() == LiveStreamStatus.ENDED)
            throw new AppException(HttpStatus.BAD_REQUEST, "Stream da ket thuc truoc do");

        live.setStatus(LiveStreamStatus.ENDED);
        live.setEndedAt(Instant.now());
        live.setViewerCount(0);
        live = liveStreamRepository.save(live);
        log.info("[Live] Ended id={} user={}", liveId, user.getUsername());

        messagingTemplate.convertAndSend("/topic/live/" + liveId + "/ended", "STREAM_ENDED");
        return buildResponse(live, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveStreamResponse> getMyStreams(User user) {
        return liveStreamRepository.findByUserOrderByStartedAtDesc(user)
                .stream().map(ls -> buildResponse(ls, null)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LiveStreamResponse getLiveByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khong tim thay nguoi dung"));
        LiveStream live = liveStreamRepository
                .findFirstByUserAndStatus(user, LiveStreamStatus.LIVE)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Nguoi dung nay chua livestream"));
        return buildResponse(live, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveStreamResponse> getLiveFeed() {
        return liveStreamRepository.findByStatusOrderByStartedAtDesc(LiveStreamStatus.LIVE)
                .stream().map(ls -> buildResponse(ls, null)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LiveStreamResponse getLiveById(Long liveId, User currentUser) {
        LiveStream live = findById(liveId);
        String token = null;
        if (currentUser != null) {
            token = zegoTokenService.generateToken04(
                    String.valueOf(currentUser.getId()),
                    live.getEffectiveRoomId(),
                    TOKEN_EFFECTIVE_SECONDS);
        }
        return buildResponse(live, token);
    }

    @Override
    @Transactional
    public LiveCommentMessage sendComment(Long liveId, SendLiveCommentRequest request, User user) {
        LiveStream live = findById(liveId);
        if (live.getStatus() == LiveStreamStatus.ENDED)
            throw new AppException(HttpStatus.BAD_REQUEST, "Khong the binh luan trong stream da ket thuc");

        LiveCommentMessage msg = LiveCommentMessage.builder()
                .liveId(liveId)
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .content(request.getContent())
                .createdAt(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/live/" + liveId + "/comments", msg);
        return msg;
    }

    @Override
    public List<LiveCommentMessage> getComments(Long liveId) {
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void incrementViewerCount(Long liveId) {
        liveStreamRepository.findById(liveId).ifPresent(ls -> {
            ls.setViewerCount(ls.getViewerCount() == null ? 1 : ls.getViewerCount() + 1);
            liveStreamRepository.save(ls);
            messagingTemplate.convertAndSend("/topic/live/" + liveId + "/viewers", ls.getViewerCount());
        });
    }

    @Override
    @Transactional
    public void decrementViewerCount(Long liveId) {
        liveStreamRepository.findById(liveId).ifPresent(ls -> {
            int count = ls.getViewerCount() == null ? 0 : Math.max(0, ls.getViewerCount() - 1);
            ls.setViewerCount(count);
            liveStreamRepository.save(ls);
            messagingTemplate.convertAndSend("/topic/live/" + liveId + "/viewers", count);
        });
    }

    private LiveStream findById(Long id) {
        return liveStreamRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khong tim thay phong live #" + id));
    }

    private LiveStreamResponse buildResponse(LiveStream ls, String zegoToken) {
        User u = ls.getUser();
        return LiveStreamResponse.builder()
                .id(ls.getId())
                .userId(u.getId())
                .username(u.getUsername())
                .avatarUrl(u.getAvatarUrl())
                .title(ls.getTitle())
                .status(ls.getStatus())
                .roomId(ls.getEffectiveRoomId())
                .zegoAppId(zegoProperties.getAppId())
                .zegoToken(zegoToken)
                .viewerCount(ls.getViewerCount())
                .startedAt(ls.getStartedAt())
                .endedAt(ls.getEndedAt())
                .build();
    }
}
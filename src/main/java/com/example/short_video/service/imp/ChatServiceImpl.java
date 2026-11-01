package com.example.short_video.service.imp;

import com.example.short_video.Enum.MessageType;
import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.response.ChatMessageResponse;
import com.example.short_video.dto.response.ConversationResponse;
import com.example.short_video.dto.response.UserSummaryResponse;
import com.example.short_video.entity.ChatMessage;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.AppException;
import com.example.short_video.repository.ChatMessageRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.repository.VideoRepository;
import com.example.short_video.service.ChatService;
import com.example.short_video.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final FollowService followService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(User sender, Long targetUserId, String content) {
        return persistMessage(sender, targetUserId, content, MessageType.TEXT, null);
    }

    @Override
    @Transactional
    public ChatMessageResponse shareVideoWithLatestChat(User sender, Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy video #" + videoId));
        if (video.getStatus() != VideoStatus.PUBLISHED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ có thể chia sẻ video đã xuất bản");
        }

        ChatMessage latestMessage = chatMessageRepository
                .findLatestMessagesByUser(sender, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(
                        HttpStatus.BAD_REQUEST,
                        "Bạn chưa có cuộc trò chuyện nào để chia sẻ video"
                ));

        Long recipientId = latestMessage.getSender().getId().equals(sender.getId())
                ? latestMessage.getReceiver().getId()
                : latestMessage.getSender().getId();
        String title = video.getTitle() == null || video.getTitle().isBlank()
                ? "Video #" + video.getId()
                : video.getTitle();

        ChatMessageResponse response = persistMessage(
                sender,
                recipientId,
                "🎥 " + title,
                MessageType.VIDEO,
                video
        );
        videoRepository.incrementShares(videoId);
        return response;
    }

    private ChatMessageResponse persistMessage(
            User sender,
            Long targetUserId,
            String content,
            MessageType messageType,
            Video video
    ) {
        if (sender.getId().equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn không thể tự gửi tin nhắn cho chính mình");
        }

        User receiver = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người nhận #" + targetUserId));

        if (Boolean.FALSE.equals(receiver.getIsActive())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản người nhận hiện đang bị khóa");
        }

        String conversationId = ChatMessage.buildConversationId(sender.getId(), receiver.getId());

        ChatMessage msg = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content.trim())
                .messageType(messageType)
                .video(video)
                .conversationId(conversationId)
                .isRead(false)
                .build();
        msg = chatMessageRepository.save(msg);

        ChatMessageResponse response = toResponse(msg, sender.getId());
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, response);
        messagingTemplate.convertAndSend("/topic/user/" + receiver.getId() + "/messages", response);

        return response;
    }

    @Override
    @Transactional
    public List<ChatMessageResponse> getMessageHistory(User currentUser, Long targetUserId, Pageable pageable) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng #" + targetUserId));

        String conversationId = ChatMessage.buildConversationId(currentUser.getId(), targetUser.getId());
        chatMessageRepository.markConversationAsRead(conversationId, currentUser);

        Page<ChatMessage> page = chatMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        List<ChatMessageResponse> list = page.getContent().stream()
                .map(m -> toResponse(m, currentUser.getId()))
                .collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(User currentUser) {
        List<String> convIds = chatMessageRepository.findConversationIdsByUser(currentUser);
        List<ConversationResponse> result = new ArrayList<>();

        for (String convId : convIds) {
            // Định dạng: "chat_{id1}_{id2}"
            String[] parts = convId.split("_");
            if (parts.length != 3) continue;

            Long u1 = Long.parseLong(parts[1]);
            Long u2 = Long.parseLong(parts[2]);
            Long otherUserId = u1.equals(currentUser.getId()) ? u2 : u1;

            Optional<User> otherUserOpt = userRepository.findById(otherUserId);
            if (otherUserOpt.isEmpty()) continue;
            User otherUser = otherUserOpt.get();

            Optional<ChatMessage> lastMsgOpt = chatMessageRepository
                    .findFirstByConversationIdOrderByCreatedAtDesc(convId);
            if (lastMsgOpt.isEmpty()) continue;

            ChatMessage lastMsg = lastMsgOpt.get();
            long unread = chatMessageRepository
                    .countByConversationIdAndReceiverAndIsReadFalse(convId, currentUser);
            boolean isMutual = followService.areMutualFriends(currentUser, otherUser);

            result.add(ConversationResponse.builder()
                    .conversationId(convId)
                    .friend(UserSummaryResponse.builder()
                            .id(otherUser.getId())
                            .username(otherUser.getUsername())
                            .avatarUrl(otherUser.getAvatarUrl())
                            .bio(otherUser.getBio())
                            .isMutual(isMutual)
                            .build())
                    .lastMessage(toResponse(lastMsg, currentUser.getId()))
                    .unreadCount(unread)
                    .updatedAt(lastMsg.getCreatedAt())
                    .build());
        }
        result.sort((a, b) -> {
            if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
            if (a.getUpdatedAt() == null) return 1;
            if (b.getUpdatedAt() == null) return -1;
            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
        });

        return result;
    }

    @Override
    @Transactional
    public void markConversationAsRead(User currentUser, Long targetUserId) {
        String conversationId = ChatMessage.buildConversationId(currentUser.getId(), targetUserId);
        chatMessageRepository.markConversationAsRead(conversationId, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(User currentUser) {
        return chatMessageRepository.countByReceiverAndIsReadFalse(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canChat(User currentUser, Long targetUserId) {
        if (currentUser.getId().equals(targetUserId)) return false;
        return userRepository.findById(targetUserId)
                .map(targetUser -> Boolean.TRUE.equals(targetUser.getIsActive()))
                .orElse(false);
    }

    private ChatMessageResponse toResponse(ChatMessage m, Long currentUserId) {
        User s = m.getSender();
        User r = m.getReceiver();
        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(s.getId())
                .senderUsername(s.getUsername())
                .senderAvatarUrl(s.getAvatarUrl())
                .receiverId(r.getId())
                .receiverUsername(r.getUsername())
                .receiverAvatarUrl(r.getAvatarUrl())
                .content(m.getContent())
                .messageType(m.getMessageType() != null ? m.getMessageType() : MessageType.TEXT)
                .videoId(m.getVideo() != null ? m.getVideo().getId() : null)
                .videoTitle(m.getVideo() != null ? m.getVideo().getTitle() : null)
                .videoThumbnailUrl(m.getVideo() != null ? m.getVideo().getThumbnailUrl() : null)
                .isRead(Boolean.TRUE.equals(m.getIsRead()))
                .createdAt(m.getCreatedAt())
                .isMine(s.getId().equals(currentUserId))
                .build();
    }
}

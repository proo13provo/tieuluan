package com.example.short_video.service;

import com.example.short_video.dto.response.ChatMessageResponse;
import com.example.short_video.dto.response.ConversationResponse;
import com.example.short_video.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {

    ChatMessageResponse sendMessage(User sender, Long targetUserId, String content);

    ChatMessageResponse shareVideoWithLatestChat(User sender, Long videoId);

    List<ChatMessageResponse> getMessageHistory(User currentUser, Long targetUserId, Pageable pageable);

    List<ConversationResponse> getConversations(User currentUser);

    void markConversationAsRead(User currentUser, Long targetUserId);

    long getTotalUnreadCount(User currentUser);

    boolean canChat(User currentUser, Long targetUserId);
}

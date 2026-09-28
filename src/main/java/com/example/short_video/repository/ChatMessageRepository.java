package com.example.short_video.repository;

import com.example.short_video.entity.ChatMessage;
import com.example.short_video.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtDesc(String conversationId);

    long countByConversationIdAndReceiverAndIsReadFalse(String conversationId, User receiver);

    long countByReceiverAndIsReadFalse(User receiver);

    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.conversationId = :convId AND m.receiver = :user AND m.isRead = false")
    void markConversationAsRead(@Param("convId") String convId, @Param("user") User user);

    @Query("SELECT DISTINCT m.conversationId FROM ChatMessage m WHERE m.sender = :user OR m.receiver = :user")
    List<String> findConversationIdsByUser(@Param("user") User user);
}

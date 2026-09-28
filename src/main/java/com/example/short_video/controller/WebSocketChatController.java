package com.example.short_video.controller;

import com.example.short_video.dto.request.SendMessageRequest;
import com.example.short_video.dto.response.ChatMessageResponse;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Controller xử lý nhận tin nhắn gửi trực tiếp qua WebSocket STOMP.
 * Client gửi tới destination: /app/chat.send
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void handleWebSocketMessage(
            @Payload SendMessageRequest request,
            Principal principal) {
        if (principal == null) {
            log.warn("[WS Chat] Reject unauthenticated message");
            return;
        }

        // Nếu principal là Authentication
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof CustomUserDetails userDetails) {
                try {
                    chatService.sendMessage(userDetails.getUser(), request.getReceiverId(), request.getContent());
                } catch (Exception e) {
                    log.error("[WS Chat] Error sending message: {}", e.getMessage());
                }
            }
        }
    }
}

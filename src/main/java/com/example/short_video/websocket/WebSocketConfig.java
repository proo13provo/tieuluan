package com.example.short_video.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.example.short_video.security.CustomUserDetailsService;
import com.example.short_video.security.JwtService;
import com.example.short_video.service.RedisTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RedisTokenService redisTokenService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwt = authHeader.substring(7);
                        try {
                            if (!redisTokenService.isBlacklisted(jwt) && redisTokenService.isAccessTokenExists(jwt)) {
                                String userEmail = jwtService.extractUsername(jwt);
                                if (userEmail != null) {
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                                    if (jwtService.isTokenValid(jwt, userDetails)) {
                                        UsernamePasswordAuthenticationToken auth =
                                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                        accessor.setUser(auth);
                                        log.info("STOMP connect authenticated user: {}", userDetails.getUsername());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("STOMP connection JWT authentication failed: {}", e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}

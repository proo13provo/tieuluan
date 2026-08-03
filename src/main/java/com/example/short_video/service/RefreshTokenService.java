package com.example.short_video.service;

import com.example.short_video.entity.RefreshToken;
import com.example.short_video.entity.User;
import com.example.short_video.exception.InvalidTokenException;
import com.example.short_video.repository.RefreshTokenRepository;
import com.example.short_video.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    /**
     * Tạo refresh token mới cho user.
     * Nếu user đã có refresh token cũ → xóa và tạo mới.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Xóa refresh token cũ nếu có
        refreshTokenRepository.findByUser(user).ifPresent(token -> {
            refreshTokenRepository.delete(token);
            refreshTokenRepository.flush();
        });

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Kiểm tra refresh token có hợp lệ không
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verify refresh token chưa hết hạn.
     * Nếu hết hạn → xóa khỏi DB và throw exception.
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }
        return token;
    }

    /**
     * Xóa refresh token của user
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}

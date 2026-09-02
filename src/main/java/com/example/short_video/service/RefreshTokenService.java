package com.example.short_video.service;

import com.example.short_video.exception.InvalidTokenException;
import com.example.short_video.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * RefreshTokenService - Tạo và xác thực refresh token dạng JWT.
 * Refresh token được lưu ở phía client (Flutter Secure Storage),
 * không lưu trong database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final JwtService jwtService;

    /**
     * Tạo refresh token (JWT) cho user.
     * Token sẽ được trả về cho FE lưu vào flutter_secure_storage.
     */
    public String createRefreshToken(UserDetails userDetails) {
        return jwtService.generateRefreshToken(userDetails);
    }

    /**
     * Xác thực refresh token.
     * Kiểm tra token hợp lệ (chữ ký đúng + chưa hết hạn).
     * Trả về email (username) nếu hợp lệ.
     */
    public String verifyRefreshToken(String refreshToken) {
        try {
            if (jwtService.isTokenExpired(refreshToken)) {
                throw new InvalidTokenException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
            }
            return jwtService.extractUsername(refreshToken);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Refresh token không hợp lệ");
        }
    }
}

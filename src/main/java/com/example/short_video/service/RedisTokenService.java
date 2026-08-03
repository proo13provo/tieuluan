package com.example.short_video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String OTP_REGISTER_PREFIX = "otp:register:";
    private static final String OTP_RESET_PREFIX = "otp:reset:";
    private static final String PENDING_USER_PREFIX = "pending_user:";
    private static final long OTP_TTL_MINUTES = 5;
    private static final long PENDING_USER_TTL_MINUTES = 10;

    // ==================== BLACKLIST ====================

    /**
     * Thêm access token vào blacklist với TTL = thời gian còn lại của token
     */
    public void blacklistAccessToken(String token, long remainingMillis) {
        if (remainingMillis > 0) {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", remainingMillis, TimeUnit.MILLISECONDS);
            log.info("Token đã được thêm vào blacklist, TTL: {}ms", remainingMillis);
        }
    }

    /**
     * Kiểm tra access token có trong blacklist không
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ==================== OTP REGISTER ====================

    /**
     * Lưu OTP đăng ký vào Redis với TTL 5 phút
     */
    public void saveRegisterOtp(String email, String otp) {
        String key = OTP_REGISTER_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("OTP đăng ký đã được lưu cho email: {}", email);
    }

    /**
     * Lấy OTP đăng ký từ Redis
     */
    public String getRegisterOtp(String email) {
        String key = OTP_REGISTER_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Xóa OTP đăng ký sau khi verify thành công
     */
    public void deleteRegisterOtp(String email) {
        String key = OTP_REGISTER_PREFIX + email;
        redisTemplate.delete(key);
    }

    // ==================== OTP RESET PASSWORD ====================

    /**
     * Lưu OTP reset password vào Redis với TTL 5 phút
     */
    public void saveResetOtp(String email, String otp) {
        String key = OTP_RESET_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("OTP reset password đã được lưu cho email: {}", email);
    }

    /**
     * Lấy OTP reset password từ Redis
     */
    public String getResetOtp(String email) {
        String key = OTP_RESET_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Xóa OTP reset password
     */
    public void deleteResetOtp(String email) {
        String key = OTP_RESET_PREFIX + email;
        redisTemplate.delete(key);
    }

    // ==================== PENDING USER ====================

    /**
     * Lưu thông tin user đang chờ verify OTP (JSON string)
     */
    public void savePendingUser(String email, String userDataJson) {
        String key = PENDING_USER_PREFIX + email;
        redisTemplate.opsForValue().set(key, userDataJson, PENDING_USER_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("Pending user đã được lưu cho email: {}", email);
    }

    /**
     * Lấy thông tin pending user
     */
    public String getPendingUser(String email) {
        String key = PENDING_USER_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Xóa pending user sau khi tạo account thành công
     */
    public void deletePendingUser(String email) {
        String key = PENDING_USER_PREFIX + email;
        redisTemplate.delete(key);
    }
}

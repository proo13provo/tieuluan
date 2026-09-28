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

    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String OTP_REGISTER_PREFIX = "otp:register:";
    private static final String OTP_RESET_PREFIX = "otp:reset:";
    private static final String PENDING_USER_PREFIX = "pending_user:";
    private static final long OTP_TTL_MINUTES = 5;
    private static final long PENDING_USER_TTL_MINUTES = 10;

    public void saveAccessToken(String token, String email, long ttlMillis) {
        if (token != null && !token.isEmpty() && ttlMillis > 0) {
            String key = ACCESS_TOKEN_PREFIX + token;
            redisTemplate.opsForValue().set(key, email, ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isAccessTokenExists(String token) {
        if (token == null || token.isEmpty()) return false;
        String key = ACCESS_TOKEN_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    public void deleteAccessToken(String token) {
        if (token != null && !token.isEmpty()) {
            String key = ACCESS_TOKEN_PREFIX + token;
            redisTemplate.delete(key);
        }
    }
    public void blacklistAccessToken(String token, long remainingMillis) {
        if (remainingMillis > 0) {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", remainingMillis, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    public void saveRegisterOtp(String email, String otp) {
        String key = OTP_REGISTER_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
    }
    public String getRegisterOtp(String email) {
        String key = OTP_REGISTER_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRegisterOtp(String email) {
        String key = OTP_REGISTER_PREFIX + email;
        redisTemplate.delete(key);
    }

    public void saveResetOtp(String email, String otp) {
        String key = OTP_RESET_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("OTP reset password đã được lưu cho email: {}", email);
    }

    public String getResetOtp(String email) {
        String key = OTP_RESET_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteResetOtp(String email) {
        String key = OTP_RESET_PREFIX + email;
        redisTemplate.delete(key);
    }

    public void savePendingUser(String email, String userDataJson) {
        String key = PENDING_USER_PREFIX + email;
        redisTemplate.opsForValue().set(key, userDataJson, PENDING_USER_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("Pending user đã được lưu cho email: {}", email);
    }
    public String getPendingUser(String email) {
        String key = PENDING_USER_PREFIX + email;
        return redisTemplate.opsForValue().get(key);
    }

    public void deletePendingUser(String email) {
        String key = PENDING_USER_PREFIX + email;
        redisTemplate.delete(key);
    }
}

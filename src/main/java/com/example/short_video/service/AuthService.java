package com.example.short_video.service;

import com.example.short_video.Enum.ERole;
import com.example.short_video.dto.request.*;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.AuthResponse;
import com.example.short_video.entity.RefreshToken;
import com.example.short_video.entity.Role;
import com.example.short_video.entity.User;
import com.example.short_video.exception.*;
import com.example.short_video.repository.RoleRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RedisTokenService redisTokenService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;

    // ==================== ĐĂNG KÝ ====================

    /**
     * Bước 1: Đăng ký - validate, lưu pending user vào Redis, gửi OTP qua email
     */
    @SneakyThrows
    public ApiResponse<Void> register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email đã được sử dụng");
        }

        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username đã được sử dụng");
        }

        // Tạo OTP 6 số
        String otp = generateOtp();

        // Lưu thông tin user tạm vào Redis (chưa tạo trong DB)
        Map<String, String> pendingData = new HashMap<>();
        pendingData.put("username", request.getUsername());
        pendingData.put("email", request.getEmail());
        pendingData.put("password", passwordEncoder.encode(request.getPassword()));

        String jsonData = objectMapper.writeValueAsString(pendingData);
        redisTokenService.savePendingUser(request.getEmail(), jsonData);

        // Lưu OTP vào Redis (TTL 5 phút)
        redisTokenService.saveRegisterOtp(request.getEmail(), otp);

        // Gửi OTP qua email
        emailService.sendRegistrationOtp(request.getEmail(), otp);

        log.info("Đã gửi OTP đăng ký đến email: {}", request.getEmail());
        return ApiResponse.success("OTP đã được gửi đến email của bạn. Vui lòng kiểm tra và xác thực trong 5 phút.");
    }

    /**
     * Bước 2: Xác thực OTP và tạo user trong database
     */
    @SneakyThrows
    @Transactional
    public ApiResponse<Void> verifyOtpAndCreateUser(VerifyOtpRequest request) {
        // Kiểm tra OTP
        String savedOtp = redisTokenService.getRegisterOtp(request.getEmail());
        if (savedOtp == null) {
            throw new InvalidOtpException("OTP đã hết hạn hoặc không tồn tại. Vui lòng đăng ký lại.");
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new InvalidOtpException("OTP không chính xác");
        }

        // Lấy thông tin pending user từ Redis
        String pendingUserJson = redisTokenService.getPendingUser(request.getEmail());
        if (pendingUserJson == null) {
            throw new InvalidOtpException("Thông tin đăng ký đã hết hạn. Vui lòng đăng ký lại.");
        }

        @SuppressWarnings("unchecked")
        Map<String, String> pendingData = objectMapper.readValue(pendingUserJson, Map.class);

        // Lấy role USER
        Role userRole = roleRepository.findByName(ERole.USER.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role USER không tồn tại trong hệ thống"));

        // Tạo user trong database
        User user = User.builder()
                .username(pendingData.get("username"))
                .email(pendingData.get("email"))
                .password(pendingData.get("password"))
                .isActive(true)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        userRepository.save(user);

        // Xóa OTP và pending user khỏi Redis
        redisTokenService.deleteRegisterOtp(request.getEmail());
        redisTokenService.deletePendingUser(request.getEmail());

        log.info("Tạo tài khoản thành công cho email: {}", request.getEmail());
        return ApiResponse.success("Đăng ký thành công! Bạn có thể đăng nhập ngay.");
    }

    // ==================== ĐĂNG NHẬP ====================

    /**
     * Đăng nhập: authenticate, tạo access token + refresh token
     */
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationFailedException("Email hoặc mật khẩu không chính xác");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Kiểm tra tài khoản active
        if (!user.getIsActive()) {
            throw new AuthenticationFailedException("Tài khoản đã bị vô hiệu hóa");
        }

        // Tạo access token
        String accessToken = jwtService.generateAccessToken(userDetails);

        // Tạo refresh token (lưu trong DB)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();

        log.info("Đăng nhập thành công cho email: {}", request.getEmail());
        return ApiResponse.success("Đăng nhập thành công", authResponse);
    }

    // ==================== REFRESH TOKEN ====================

    /**
     * Làm mới access token bằng refresh token.
     * Blacklist access token cũ vào Redis.
     */
    public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request, String oldAccessToken) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token không hợp lệ"));

        // Verify refresh token chưa hết hạn
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Blacklist access token cũ (nếu có)
        blacklistTokenIfPresent(oldAccessToken);

        // Tạo access token mới
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .build();

        log.info("Refresh token thành công cho user: {}", user.getEmail());
        return ApiResponse.success("Token đã được làm mới", authResponse);
    }

    // ==================== ĐĂNG XUẤT ====================

    /**
     * Đăng xuất: blacklist access token + xóa refresh token
     */
    @Transactional
    public ApiResponse<Void> logout(String accessToken, LogoutRequest request) {
        // Blacklist access token
        blacklistTokenIfPresent(accessToken);

        // Xóa refresh token khỏi DB
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElse(null);
        if (refreshToken != null) {
            refreshTokenService.deleteByUser(refreshToken.getUser());
        }

        log.info("Đăng xuất thành công");
        return ApiResponse.success("Đăng xuất thành công");
    }

    // ==================== QUÊN MẬT KHẨU ====================

    /**
     * Bước 1: Gửi OTP reset password qua email
     */
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        // Kiểm tra email tồn tại
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email không tồn tại trong hệ thống");
        }

        // Tạo OTP 6 số
        String otp = generateOtp();

        // Lưu OTP vào Redis (TTL 5 phút)
        redisTokenService.saveResetOtp(request.getEmail(), otp);

        // Gửi OTP qua email
        emailService.sendResetPasswordOtp(request.getEmail(), otp);

        log.info("Đã gửi OTP reset password đến email: {}", request.getEmail());
        return ApiResponse.success("OTP đặt lại mật khẩu đã được gửi đến email của bạn.");
    }

    /**
     * Bước 2: Xác thực OTP và đặt lại mật khẩu mới
     */
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        // Kiểm tra OTP
        String savedOtp = redisTokenService.getResetOtp(request.getEmail());
        if (savedOtp == null) {
            throw new InvalidOtpException("OTP đã hết hạn hoặc không tồn tại");
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new InvalidOtpException("OTP không chính xác");
        }

        // Tìm user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Cập nhật mật khẩu
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa OTP khỏi Redis
        redisTokenService.deleteResetOtp(request.getEmail());

        // Xóa refresh token cũ (force re-login)
        refreshTokenService.deleteByUser(user);

        log.info("Đặt lại mật khẩu thành công cho email: {}", request.getEmail());
        return ApiResponse.success("Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.");
    }

    // ==================== UTILITY ====================

    /**
     * Tạo OTP 6 số ngẫu nhiên
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Blacklist access token nếu có, bỏ qua lỗi nếu token đã hết hạn
     */
    private void blacklistTokenIfPresent(String accessToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                long remainingMillis = jwtService.getRemainingExpiration(accessToken);
                redisTokenService.blacklistAccessToken(accessToken, remainingMillis);
            } catch (Exception e) {
                log.warn("Không thể blacklist access token: {}", e.getMessage());
            }
        }
    }
}

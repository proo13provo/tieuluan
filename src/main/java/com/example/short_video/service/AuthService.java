package com.example.short_video.service;

import com.example.short_video.Enum.AuthProvider;
import com.example.short_video.Enum.ERole;
import com.example.short_video.dto.request.*;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.AuthResponse;
import com.example.short_video.entity.Role;
import com.example.short_video.entity.User;
import com.example.short_video.entity.UserAuthProvider;
import com.example.short_video.exception.*;
import com.example.short_video.repository.RoleRepository;
import com.example.short_video.repository.UserAuthProviderRepository;
import com.example.short_video.repository.UserRepository;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.security.JwtService;
import com.example.short_video.util.generateOtp;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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
    private final UserAuthProviderRepository userAuthProviderRepository;
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
        String otp = generateOtp.RandomOtp();

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

        // Tạo auth provider LOCAL cho user
        UserAuthProvider localProvider = UserAuthProvider.builder()
                .user(user)
                .provider(AuthProvider.LOCAL)
                .providerUid(null)
                .build();
        userAuthProviderRepository.save(localProvider);

        // Xóa OTP và pending user khỏi Redis
        redisTokenService.deleteRegisterOtp(request.getEmail());
        redisTokenService.deletePendingUser(request.getEmail());

        return ApiResponse.success("Đăng ký thành công! Bạn có thể đăng nhập ngay.");
    }

    // ==================== ĐĂNG NHẬP ====================

    /**
     * Đăng nhập: authenticate, tạo access token + refresh token (JWT)
     * Lưu access token vào Redis với TTL
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

        // Lưu access token vào Redis với TTL
        redisTokenService.saveAccessToken(accessToken, user.getEmail(), jwtService.getAccessTokenExpiration());

        // Tạo refresh token (JWT - FE lưu vào flutter_secure_storage)
        String refreshToken = refreshTokenService.createRefreshToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return ApiResponse.success("Đăng nhập thành công", authResponse);
    }

    // ==================== ĐĂNG NHẬP GOOGLE ====================

    /**
     * Đăng nhập bằng Google:
     * 1. Verify Google ID Token
     * 2. Tìm user theo email:
     *    - Chưa có: Tạo user mới + AuthProvider(GOOGLE)
     *    - Đã có + chưa link GOOGLE: Thêm AuthProvider(GOOGLE) → link account
     *    - Đã có + đã link GOOGLE: Đăng nhập luôn
     * 3. Trả JWT tokens
     */
    @Transactional
    public ApiResponse<AuthResponse> googleLogin(GoogleLoginRequest request) {
        // Verify Firebase ID Token
        FirebaseToken firebaseToken = verifyFirebaseIdToken(request.getIdToken());

        String email = firebaseToken.getEmail();
        String googleUid = firebaseToken.getUid();
        String name = firebaseToken.getName();
        String pictureUrl = firebaseToken.getPicture();

        // Tìm user theo email
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        User user;
        if (existingUserOpt.isPresent()) {
            // User đã tồn tại
            user = existingUserOpt.get();

            // Kiểm tra tài khoản active
            if (!user.getIsActive()) {
                throw new AuthenticationFailedException("Tài khoản đã bị vô hiệu hóa");
            }

            // Link Google provider nếu chưa có
            if (!userAuthProviderRepository.existsByUserAndProvider(user, AuthProvider.GOOGLE)) {
                UserAuthProvider googleProvider = UserAuthProvider.builder()
                        .user(user)
                        .provider(AuthProvider.GOOGLE)
                        .providerUid(googleUid)
                        .build();
                userAuthProviderRepository.save(googleProvider);
                log.info("Linked Google account to existing user: {}", email);
            }
        } else {
            // User chưa tồn tại → tạo mới
            Role userRole = roleRepository.findByName(ERole.USER.name())
                    .orElseThrow(() -> new ResourceNotFoundException("Role USER không tồn tại trong hệ thống"));

            // Tạo username từ email (phần trước @) + random suffix để tránh trùng
            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            if (userRepository.existsByUsername(username)) {
                username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 6);
            }

            user = User.builder()
                    .username(username)
                    .email(email)
                    .password(null) // Google user không có password local
                    .avatarUrl(pictureUrl)
                    .isActive(true)
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();

            userRepository.save(user);

            // Tạo auth provider GOOGLE
            UserAuthProvider googleProvider = UserAuthProvider.builder()
                    .user(user)
                    .provider(AuthProvider.GOOGLE)
                    .providerUid(googleUid)
                    .build();
            userAuthProviderRepository.save(googleProvider);

            log.info("Created new user via Google login: {}", email);
        }

        // Tạo JWT tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        redisTokenService.saveAccessToken(accessToken, user.getEmail(), jwtService.getAccessTokenExpiration());

        String refreshToken = refreshTokenService.createRefreshToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return ApiResponse.success("Đăng nhập Google thành công", authResponse);
    }

    /**
     * Verify Firebase ID Token bằng Firebase Admin SDK
     */
    private FirebaseToken verifyFirebaseIdToken(String idTokenString) {
        try {
            FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(idTokenString);

            // Kiểm tra email tồn tại
            if (firebaseToken.getEmail() == null || firebaseToken.getEmail().isEmpty()) {
                throw new AuthenticationFailedException("Firebase token không chứa email");
            }

            // Kiểm tra email đã verified
            if (!firebaseToken.isEmailVerified()) {
                throw new AuthenticationFailedException("Email chưa được xác thực");
            }

            return firebaseToken;
        } catch (AuthenticationFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi verify Firebase ID Token: {}", e.getMessage());
            throw new AuthenticationFailedException("Không thể xác thực Firebase ID Token");
        }
    }

    // ==================== REFRESH TOKEN ====================

    /**
     * Làm mới access token bằng refresh token.
     * Refresh token là JWT, verify bằng chữ ký + expiry.
     * Xóa access token cũ khỏi Redis và blacklist nó.
     * Lưu access token mới vào Redis.
     */
    public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request, String oldAccessToken) {
        // Verify refresh token (JWT) - trả về email nếu hợp lệ
        String email = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Hủy access token cũ (nếu có)
        if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
            redisTokenService.deleteAccessToken(oldAccessToken);
            try {
                long remainingMillis = jwtService.getRemainingExpiration(oldAccessToken);
                if (remainingMillis > 0) {
                    redisTokenService.blacklistAccessToken(oldAccessToken, remainingMillis);
                }
            } catch (Exception ignored) {}
        }

        // Tạo access token mới
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        // Lưu access token mới vào Redis
        redisTokenService.saveAccessToken(newAccessToken, email, jwtService.getAccessTokenExpiration());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken()) // Giữ nguyên refresh token cũ (chưa hết hạn)
                .build();

        return ApiResponse.success("Token đã được làm mới", authResponse);
    }

    // ==================== ĐĂNG XUẤT ====================

    /**
     * Đăng xuất: xóa access token khỏi Redis + blacklist access token & refresh token
     * FE sẽ xóa refresh token khỏi flutter_secure_storage
     */
    @Transactional
    public ApiResponse<Void> logout(String accessToken, LogoutRequest request) {
        // Xóa và blacklist access token
        if (accessToken != null && !accessToken.isEmpty()) {
            redisTokenService.deleteAccessToken(accessToken);
            try {
                long remainingMillis = jwtService.getRemainingExpiration(accessToken);
                if (remainingMillis > 0) {
                    redisTokenService.blacklistAccessToken(accessToken, remainingMillis);
                }
            } catch (Exception ignored) {}
        }

        // Blacklist refresh token vào Redis (để ngăn dùng lại)
        try {
            String refreshToken = request.getRefreshToken();
            if (refreshToken != null && !refreshToken.isEmpty()) {
                long remainingMillis = jwtService.getRemainingExpiration(refreshToken);
                if (remainingMillis > 0) {
                    redisTokenService.blacklistAccessToken(refreshToken, remainingMillis);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể blacklist refresh token: {}", e.getMessage());
        }

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
        String otp = generateOtp.RandomOtp();

        // Lưu OTP vào Redis (TTL 5 phút)
        redisTokenService.saveResetOtp(request.getEmail(), otp);

        // Gửi OTP qua email
        emailService.sendResetPasswordOtp(request.getEmail(), otp);

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

        // Nếu chưa có LOCAL provider thì thêm (trường hợp user Google muốn đặt password)
        if (!userAuthProviderRepository.existsByUserAndProvider(user, AuthProvider.LOCAL)) {
            UserAuthProvider localProvider = UserAuthProvider.builder()
                    .user(user)
                    .provider(AuthProvider.LOCAL)
                    .providerUid(null)
                    .build();
            userAuthProviderRepository.save(localProvider);
        }

        // Xóa OTP khỏi Redis
        redisTokenService.deleteResetOtp(request.getEmail());

        return ApiResponse.success("Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.");
    }
}

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

    @SneakyThrows
    public ApiResponse<Void> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email đã được sử dụng");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username đã được sử dụng");
        }
        String otp = generateOtp.RandomOtp();
        Map<String, String> pendingData = new HashMap<>();
        pendingData.put("username", request.getUsername());
        pendingData.put("email", request.getEmail());
        pendingData.put("password", passwordEncoder.encode(request.getPassword()));

        String jsonData = objectMapper.writeValueAsString(pendingData);
        redisTokenService.savePendingUser(request.getEmail(), jsonData);
        redisTokenService.saveRegisterOtp(request.getEmail(), otp);
        emailService.sendRegistrationOtp(request.getEmail(), otp);
        return ApiResponse.success("OTP đã được gửi đến email của bạn. Vui lòng kiểm tra và xác thực trong 5 phút.");
    }

    @SneakyThrows
    @Transactional
    public ApiResponse<Void> verifyOtpAndCreateUser(VerifyOtpRequest request) {
        String savedOtp = redisTokenService.getRegisterOtp(request.getEmail());
        if (savedOtp == null) {
            throw new InvalidOtpException("OTP đã hết hạn hoặc không tồn tại. Vui lòng đăng ký lại.");
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new InvalidOtpException("OTP không chính xác");
        }
        String pendingUserJson = redisTokenService.getPendingUser(request.getEmail());
        if (pendingUserJson == null) {
            throw new InvalidOtpException("Thông tin đăng ký đã hết hạn. Vui lòng đăng ký lại.");
        }

        @SuppressWarnings("unchecked")
        Map<String, String> pendingData = objectMapper.readValue(pendingUserJson, Map.class);

        Role userRole = roleRepository.findByName(ERole.USER.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role USER không tồn tại trong hệ thống"));
        User user = User.builder()
                .username(pendingData.get("username"))
                .email(pendingData.get("email"))
                .password(pendingData.get("password"))
                .isActive(true)
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        userRepository.save(user);
        UserAuthProvider localProvider = UserAuthProvider.builder()
                .user(user)
                .provider(AuthProvider.LOCAL)
                .providerUid(null)
                .build();
        userAuthProviderRepository.save(localProvider);
        redisTokenService.deleteRegisterOtp(request.getEmail());
        redisTokenService.deletePendingUser(request.getEmail());

        return ApiResponse.success("Đăng ký thành công! Bạn có thể đăng nhập ngay.");
    }
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
        if (!user.getIsActive()) {
            throw new AuthenticationFailedException("Tài khoản đã bị vô hiệu hóa");
        }
        String accessToken = jwtService.generateAccessToken(userDetails);
        redisTokenService.saveAccessToken(accessToken, user.getEmail(), jwtService.getAccessTokenExpiration());
        String refreshToken = refreshTokenService.createRefreshToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return ApiResponse.success("Đăng nhập thành công", authResponse);
    }
    @Transactional
    public ApiResponse<AuthResponse> googleLogin(GoogleLoginRequest request) {
        FirebaseToken firebaseToken = verifyFirebaseIdToken(request.getIdToken());

        String email = firebaseToken.getEmail();
        String googleUid = firebaseToken.getUid();
        String pictureUrl = firebaseToken.getPicture();

        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (!user.getIsActive()) {
                throw new AuthenticationFailedException("Tài khoản đã bị vô hiệu hóa");
            }
            if (!userAuthProviderRepository.existsByUserAndProvider(user, AuthProvider.GOOGLE)) {
                UserAuthProvider googleProvider = UserAuthProvider.builder()
                        .user(user)
                        .provider(AuthProvider.GOOGLE)
                        .providerUid(googleUid)
                        .build();
                userAuthProviderRepository.save(googleProvider);
            }
        } else {
            Role userRole = roleRepository.findByName(ERole.USER.name())
                    .orElseThrow(() -> new ResourceNotFoundException("Role USER không tồn tại trong hệ thống"));
            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            if (userRepository.existsByUsername(username)) {
                username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 6);
            }

            user = User.builder()
                    .username(username)
                    .email(email)
                    .password(null)
                    .avatarUrl(pictureUrl)
                    .isActive(true)
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();

            userRepository.save(user);
            UserAuthProvider googleProvider = UserAuthProvider.builder()
                    .user(user)
                    .provider(AuthProvider.GOOGLE)
                    .providerUid(googleUid)
                    .build();
            userAuthProviderRepository.save(googleProvider);

        }
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
    public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request, String oldAccessToken) {
        String email = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        blacklistToken(oldAccessToken);
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        redisTokenService.saveAccessToken(newAccessToken, email, jwtService.getAccessTokenExpiration());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .build();

        return ApiResponse.success("Token đã được làm mới", authResponse);
    }
    @Transactional
    public ApiResponse<Void> logout(String accessToken, LogoutRequest request) {
        blacklistToken(accessToken);
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

    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email không tồn tại trong hệ thống");
        }
        String otp = generateOtp.RandomOtp();
        redisTokenService.saveResetOtp(request.getEmail(), otp);
        emailService.sendResetPasswordOtp(request.getEmail(), otp);
        return ApiResponse.success("OTP đặt lại mật khẩu đã được gửi đến email của bạn.");
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        String savedOtp = redisTokenService.getResetOtp(request.getEmail());
        if (savedOtp == null) {
            throw new InvalidOtpException("OTP đã hết hạn hoặc không tồn tại");
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new InvalidOtpException("OTP không chính xác");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        if (!userAuthProviderRepository.existsByUserAndProvider(user, AuthProvider.LOCAL)) {
            UserAuthProvider localProvider = UserAuthProvider.builder()
                    .user(user)
                    .provider(AuthProvider.LOCAL)
                    .providerUid(null)
                    .build();
            userAuthProviderRepository.save(localProvider);
        }
        redisTokenService.deleteResetOtp(request.getEmail());

        return ApiResponse.success("Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.");
    }

    private void blacklistToken(String token) {
        if (token == null || token.isEmpty()) { return; }
        redisTokenService.deleteAccessToken(token);
        try {
            long remainingMillis = jwtService.getRemainingExpiration(token);
            if (remainingMillis > 0) {
                redisTokenService.blacklistAccessToken(token, remainingMillis);
            }
        }catch (Exception ignored) {}
    }
}

package com.example.short_video.controller;

import com.example.short_video.dto.request.*;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.AuthResponse;
import com.example.short_video.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Xác thực (Auth)", description = "Các API đăng ký, đăng nhập, OTP, Google login, refresh token")
public class AuthController {

    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtpAndCreateUser(request));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String oldAccessToken = extractTokenFromRequest(httpRequest);
        return ResponseEntity.ok(authService.refreshToken(request, oldAccessToken));
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletRequest httpRequest) {

        String accessToken = extractTokenFromRequest(httpRequest);
        return ResponseEntity.ok(authService.logout(accessToken, request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

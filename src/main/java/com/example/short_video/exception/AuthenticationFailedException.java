package com.example.short_video.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception dùng khi xác thực thất bại (sai email/password, tài khoản bị vô hiệu hóa)
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}

package com.example.short_video.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception dùng khi OTP không hợp lệ, hết hạn, hoặc sai
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }
}

package com.example.short_video.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String fromEmail;

    /**
     * Gửi email chứa OTP đăng ký
     */
    @Async
    public void sendRegistrationOtp(String toEmail, String otp) {
        String subject = "Short Video - Mã xác thực đăng ký";
        String body = buildOtpEmailBody("Xác thực đăng ký tài khoản", otp);
        sendEmail(toEmail, subject, body);
    }

    /**
     * Gửi email chứa OTP reset password
     */
    @Async
    public void sendResetPasswordOtp(String toEmail, String otp) {
        String subject = "Short Video - Mã xác thực đặt lại mật khẩu";
        String body = buildOtpEmailBody("Đặt lại mật khẩu", otp);
        sendEmail(toEmail, subject, body);
    }

    /**
     * Gửi email
     */
    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email đã gửi thành công đến: {}", to);
        } catch (MessagingException e) {
            log.error("Lỗi gửi email đến {}: {}", to, e.getMessage());
            throw new com.example.short_video.exception.EmailSendException("Không thể gửi email", e);
        }
    }

    /**
     * Tạo nội dung HTML cho email OTP
     */
    private String buildOtpEmailBody(String title, String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                    <div style="max-width: 500px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                        <h2 style="color: #333; text-align: center;">🎬 Short Video</h2>
                        <h3 style="color: #555; text-align: center;">%s</h3>
                        <p style="color: #666; text-align: center;">Mã OTP của bạn là:</p>
                        <div style="background-color: #f0f0f0; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #333;">%s</span>
                        </div>
                        <p style="color: #999; text-align: center; font-size: 14px;">
                            Mã này sẽ hết hạn sau <strong>5 phút</strong>.<br>
                            Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(title, otp);
    }
}

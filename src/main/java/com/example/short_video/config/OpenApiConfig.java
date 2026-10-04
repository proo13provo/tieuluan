package com.example.short_video.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Short Video Application API",
                version = "1.0.0",
                description = "Tài liệu API cho ứng dụng video ngắn (TikTok clone) bao gồm Video, Tương tác (Like, Comment, Follow), LiveStream, Chat và Xác thực.",
                contact = @Contact(
                        name = "Đội ngũ phát triển",
                        email = "support@example.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://springdoc.org"
                )
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Nhập JWT Access Token vào đây (không cần tiền tố 'Bearer ')"
)
public class OpenApiConfig {
}

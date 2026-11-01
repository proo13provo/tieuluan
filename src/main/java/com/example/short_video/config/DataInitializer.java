package com.example.short_video.config;

import com.example.short_video.Enum.AuthProvider;
import com.example.short_video.Enum.ERole;
import com.example.short_video.entity.Category;
import com.example.short_video.entity.Role;
import com.example.short_video.entity.User;
import com.example.short_video.entity.UserAuthProvider;
import com.example.short_video.repository.CategoryRepository;
import com.example.short_video.repository.RoleRepository;
import com.example.short_video.repository.UserAuthProviderRepository;
import com.example.short_video.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Khởi tạo các Role hệ thống
        for (ERole eRole : ERole.values()) {
            if (roleRepository.findByName(eRole.name()).isEmpty()) {
                roleRepository.save(Role.builder().name(eRole.name()).build());
                log.info("Đã tạo role: {}", eRole.name());
            }
        }

        // 2. Khởi tạo danh mục mặc định
        for (com.example.short_video.Enum.Category cat : com.example.short_video.Enum.Category.values()) {
            if (categoryRepository.findByName(cat.name()).isEmpty()) {
                categoryRepository.save(Category.builder().name(cat.name()).build());
                log.info("Đã tạo category: {}", cat.name());
            }
        }

        // 3. Khởi tạo tài khoản Quản trị viên (Admin) mặc định nếu chưa có
        String adminEmail = "admin@shortvideo.com";
        if (userRepository.findByEmail(adminEmail).isEmpty() && !userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName(ERole.ADMIN.name()).orElse(null);
            Role modRole = roleRepository.findByName(ERole.MODERATOR.name()).orElse(null);
            Role userRole = roleRepository.findByName(ERole.USER.name()).orElse(null);

            Set<Role> roles = new HashSet<>();
            if (adminRole != null) roles.add(adminRole);
            if (modRole != null) roles.add(modRole);
            if (userRole != null) roles.add(userRole);

            User admin = User.builder()
                    .username("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@123456"))
                    .isActive(true)
                    .roles(roles)
                    .warningCount(0)
                    .bio("Quản trị viên hệ thống")
                    .build();

            User savedAdmin = userRepository.save(admin);
            userAuthProviderRepository.save(UserAuthProvider.builder()
                    .user(savedAdmin)
                    .provider(AuthProvider.LOCAL)
                    .build());
            log.info("Đã tạo tài khoản Quản trị viên mặc định: {} / Admin@123456", adminEmail);
        }
    }
}

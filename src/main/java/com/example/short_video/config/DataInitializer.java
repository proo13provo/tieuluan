package com.example.short_video.config;

import com.example.short_video.Enum.ERole;
import com.example.short_video.entity.Role;
import com.example.short_video.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (ERole eRole : ERole.values()) {
            if (roleRepository.findByName(eRole.name()).isEmpty()) {
                roleRepository.save(Role.builder().name(eRole.name()).build());
                log.info("Đã tạo role: {}", eRole.name());
            }
        }
        log.info("Khởi tạo dữ liệu role hoàn tất");
    }
}

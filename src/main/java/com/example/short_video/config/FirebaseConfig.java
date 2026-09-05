package com.example.short_video.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getServiceAccountStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    private InputStream getServiceAccountStream() throws IOException {
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            File file = new File(serviceAccountPath);
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            }

            String cleanPath = serviceAccountPath.replace("classpath:", "");
            if (cleanPath.startsWith("/")) {
                cleanPath = cleanPath.substring(1);
            }
            ClassPathResource classPathResource = new ClassPathResource(cleanPath);
            if (classPathResource.exists()) {
                return classPathResource.getInputStream();
            }
        }

        ClassPathResource defaultResource = new ClassPathResource("firebase-service-account.json");
        if (defaultResource.exists()) {
            return defaultResource.getInputStream();
        }

        throw new FileNotFoundException("Không tìm thấy file Firebase Service Account tại: " + serviceAccountPath);
    }
}


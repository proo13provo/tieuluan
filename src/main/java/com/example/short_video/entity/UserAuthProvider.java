package com.example.short_video.entity;

import com.example.short_video.Enum.AuthProvider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_auth_providers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_uid", length = 255)
    private String providerUid;
}

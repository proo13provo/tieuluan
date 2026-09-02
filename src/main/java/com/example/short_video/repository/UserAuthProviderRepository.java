package com.example.short_video.repository;

import com.example.short_video.Enum.AuthProvider;
import com.example.short_video.entity.User;
import com.example.short_video.entity.UserAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    Optional<UserAuthProvider> findByUserAndProvider(User user, AuthProvider provider);

    List<UserAuthProvider> findAllByUser(User user);

    boolean existsByUserAndProvider(User user, AuthProvider provider);
}

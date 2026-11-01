package com.example.short_video.repository;

import com.example.short_video.entity.Role;
import com.example.short_video.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByIsActive(Boolean isActive);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<User> findByIsActiveOrderByCreatedAtDesc(Boolean isActive, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.username) LIKE :pattern OR LOWER(u.email) LIKE :pattern) AND " +
           "u.isActive = :isActive")
    Page<User> searchUsersByPatternAndStatus(@Param("pattern") String pattern, @Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE :pattern OR LOWER(u.email) LIKE :pattern")
    Page<User> searchUsersByPattern(@Param("pattern") String pattern, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.username) LIKE :pattern OR LOWER(u.bio) LIKE :pattern)")
    Page<User> searchPublicUsersByPattern(@Param("pattern") String pattern, Pageable pageable);

    List<User> findByIsActiveTrue();

    List<User> findTop10ByOrderByCreatedAtDesc();
}

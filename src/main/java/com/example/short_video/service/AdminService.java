package com.example.short_video.service;

import com.example.short_video.Enum.ERole;
import com.example.short_video.Enum.ReportStatus;
import com.example.short_video.Enum.VideoStatus;
import com.example.short_video.dto.request.CategoryRequest;
import com.example.short_video.dto.request.HashtagRequest;
import com.example.short_video.dto.request.UpdateUserRolesRequest;
import com.example.short_video.dto.request.UpdateUserStatusRequest;
import com.example.short_video.dto.response.AdminUserResponse;
import com.example.short_video.dto.response.ApiResponse;
import com.example.short_video.dto.response.DashboardStatsResponse;
import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.entity.Category;
import com.example.short_video.entity.Hashtag;
import com.example.short_video.entity.Role;
import com.example.short_video.entity.User;
import com.example.short_video.entity.Video;
import com.example.short_video.exception.ResourceAlreadyExistsException;
import com.example.short_video.exception.ResourceNotFoundException;
import com.example.short_video.mapper.VideoMapper;
import com.example.short_video.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final HashtagRepository hashtagRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final VideoMapper videoMapper;


    @Transactional
    public ApiResponse<AdminUserResponse> updateUserStatus(Long userId, UpdateUserStatusRequest request, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        if (user.getId().equals(actor.getId()) && Boolean.FALSE.equals(request.getIsActive())) {
            throw new IllegalArgumentException("Không thể tự khóa tài khoản của chính mình");
        }

        boolean isTargetAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));
        boolean isActorAdmin = actor.getRoles() != null && actor.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        if (isTargetAdmin && !isActorAdmin && Boolean.FALSE.equals(request.getIsActive())) {
            throw new IllegalArgumentException("Kiểm duyệt viên không có quyền khóa tài khoản của Quản trị viên");
        }

        user.setIsActive(request.getIsActive());
        User updated = userRepository.save(user);

        String action = Boolean.TRUE.equals(request.getIsActive()) ? "mở khóa" : "khóa";
        log.warn("User {} ({}) đã {} tài khoản user ID {}. Lý do: {}",
                actor.getUsername(), isActorAdmin ? "ADMIN" : "MODERATOR", action, userId, request.getReason());

        return ApiResponse.success("Đã " + action + " tài khoản người dùng thành công", mapToAdminUserResponse(updated));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<AdminUserResponse>> getUsers(String keyword, Boolean isActive, int page, int size) {
        int targetPage = Math.max(0, page);
        int targetSize = (size > 0 && size <= 100) ? size : 20;
        Pageable pageable = PageRequest.of(targetPage, targetSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<User> users;
        if (cleanKeyword != null) {
            String pattern = "%" + cleanKeyword.toLowerCase() + "%";
            if (isActive != null) {
                users = userRepository.searchUsersByPatternAndStatus(pattern, isActive, pageable);
            } else {
                users = userRepository.searchUsersByPattern(pattern, pageable);
            }
        } else {
            if (isActive != null) {
                users = userRepository.findByIsActiveOrderByCreatedAtDesc(isActive, pageable);
            } else {
                users = userRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        }

        Page<AdminUserResponse> responsePage = users.map(this::mapToAdminUserResponse);
        return ApiResponse.success("Lấy danh sách người dùng thành công", responsePage);
    }


    @Transactional
    public ApiResponse<AdminUserResponse> updateUserRoles(Long userId, UpdateUserRolesRequest request, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Set<String> targetRoleNames = request.getRoles().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        // Kiểm tra an toàn: Không cho phép Admin tự thu hồi quyền ADMIN của chính mình
        if (user.getId().equals(admin.getId()) && !targetRoleNames.contains("ADMIN")) {
            throw new IllegalArgumentException("Quản trị viên không thể tự thu hồi quyền ADMIN của chính mình");
        }

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : targetRoleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Vai trò không hợp lệ: " + roleName));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        User updated = userRepository.save(user);

        log.info("Admin {} đã cập nhật vai trò người dùng ID {} thành: {}",
                admin.getUsername(), userId, targetRoleNames);

        return ApiResponse.success("Cập nhật vai trò người dùng thành công", mapToAdminUserResponse(updated));
    }

    @Transactional
    public ApiResponse<AdminUserResponse> grantModeratorRole(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Role modRole = roleRepository.findByName(ERole.MODERATOR.name())
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò MODERATOR không tồn tại trong hệ thống"));

        Set<Role> roles = user.getRoles() != null ? new HashSet<>(user.getRoles()) : new HashSet<>();
        boolean alreadyModerator = roles.stream().anyMatch(r -> ERole.MODERATOR.name().equalsIgnoreCase(r.getName()));

        if (alreadyModerator) {
            return ApiResponse.success("Người dùng này đã có quyền Moderator từ trước", mapToAdminUserResponse(user));
        }

        roles.add(modRole);
        user.setRoles(roles);
        User updated = userRepository.save(user);

        log.info("Admin {} đã cấp quyền MODERATOR cho người dùng {} (ID: {})",
                admin.getUsername(), user.getUsername(), userId);

        return ApiResponse.success("Cấp quyền Moderator cho người dùng thành công", mapToAdminUserResponse(updated));
    }

    @Transactional
    public ApiResponse<AdminUserResponse> revokeModeratorRole(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Set<Role> roles = user.getRoles() != null ? new HashSet<>(user.getRoles()) : new HashSet<>();
        boolean wasRemoved = roles.removeIf(r -> ERole.MODERATOR.name().equalsIgnoreCase(r.getName()));

        if (!wasRemoved) {
            return ApiResponse.success("Người dùng này chưa có quyền Moderator", mapToAdminUserResponse(user));
        }

        // Đảm bảo user vẫn còn role USER nếu danh sách roles trống
        if (roles.isEmpty()) {
            roleRepository.findByName(ERole.USER.name()).ifPresent(roles::add);
        }

        user.setRoles(roles);
        User updated = userRepository.save(user);

        log.info("Admin {} đã thu hồi quyền MODERATOR của người dùng {} (ID: {})",
                admin.getUsername(), user.getUsername(), userId);

        return ApiResponse.success("Thu hồi quyền Moderator của người dùng thành công", mapToAdminUserResponse(updated));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<Role>> getAllRoles() {
        return ApiResponse.success("Lấy danh sách vai trò thành công", roleRepository.findAll());
    }


    @Transactional(readOnly = true)
    public ApiResponse<List<Category>> getAllCategories() {
        return ApiResponse.success("Lấy danh sách danh mục thành công", categoryRepository.findAll());
    }

    @Transactional
    public ApiResponse<Category> createCategory(CategoryRequest request) {
        String name = request.getName().trim().toUpperCase();
        if (categoryRepository.existsByName(name)) {
            throw new ResourceAlreadyExistsException("Danh mục với tên '" + name + "' đã tồn tại");
        }

        Category category = Category.builder().name(name).build();
        Category saved = categoryRepository.save(category);
        log.info("Đã tạo danh mục mới: {}", name);

        return ApiResponse.success("Tạo danh mục mới thành công", saved);
    }

    @Transactional
    public ApiResponse<Category> updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        String newName = request.getName().trim().toUpperCase();
        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByName(newName)) {
            throw new ResourceAlreadyExistsException("Danh mục với tên '" + newName + "' đã tồn tại");
        }

        category.setName(newName);
        Category saved = categoryRepository.save(category);
        log.info("Đã cập nhật danh mục ID {} thành: {}", id, newName);

        return ApiResponse.success("Cập nhật danh mục thành công", saved);
    }

    @Transactional
    public ApiResponse<Void> deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        categoryRepository.delete(category);
        log.warn("Đã xóa danh mục ID {}: {}", id, category.getName());

        return ApiResponse.success("Xóa danh mục thành công");
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<Hashtag>> getAllHashtags(String keyword, int page, int size) {
        int targetPage = Math.max(0, page);
        int targetSize = (size > 0 && size <= 100) ? size : 20;
        Pageable pageable = PageRequest.of(targetPage, targetSize, Sort.by(Sort.Direction.DESC, "usageCount"));

        Page<Hashtag> hashtags;
        if (keyword != null && !keyword.isBlank()) {
            String clean = keyword.trim().toLowerCase();
            if (clean.startsWith("#")) clean = clean.substring(1);
            hashtags = hashtagRepository.findByNameContainingIgnoreCase(clean, pageable);
        } else {
            hashtags = hashtagRepository.findAll(pageable);
        }

        return ApiResponse.success("Lấy danh sách hashtag thành công", hashtags);
    }

    @Transactional
    public ApiResponse<Hashtag> updateHashtag(Long id, HashtagRequest request) {
        Hashtag hashtag = hashtagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hashtag với ID: " + id));

        String newName = request.getName().trim().toLowerCase();
        if (newName.startsWith("#")) newName = newName.substring(1);

        if (!hashtag.getName().equalsIgnoreCase(newName) && hashtagRepository.existsByName(newName)) {
            throw new ResourceAlreadyExistsException("Hashtag #" + newName + " đã tồn tại");
        }

        hashtag.setName(newName);
        Hashtag updated = hashtagRepository.save(hashtag);
        log.info("Đã cập nhật hashtag ID {} thành: #{}", id, newName);

        return ApiResponse.success("Cập nhật hashtag thành công", updated);
    }

    @Transactional
    public ApiResponse<Void> deleteHashtag(Long id) {
        Hashtag hashtag = hashtagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hashtag với ID: " + id));

        hashtagRepository.delete(hashtag);
        log.warn("Đã xóa hashtag ID {}: #{}", id, hashtag.getName());

        return ApiResponse.success("Xóa hashtag thành công");
    }


    @Transactional(readOnly = true)
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long lockedUsers = userRepository.countByIsActive(false);

        DashboardStatsResponse.UserStats userStats = DashboardStatsResponse.UserStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .lockedUsers(lockedUsers)
                .build();

        long totalVideos = videoRepository.count();
        long publishedVideos = videoRepository.countByStatus(VideoStatus.PUBLISHED);
        long hiddenVideos = videoRepository.countByStatus(VideoStatus.HIDDEN);
        long deletedVideos = videoRepository.countByStatus(VideoStatus.DELETED);
        long draftVideos = videoRepository.countByStatus(VideoStatus.DRAFT);
        long totalViews = videoRepository.sumTotalViews();
        long totalLikes = videoRepository.sumTotalLikes();
        long totalShares = videoRepository.sumTotalShares();
        long totalComments = commentRepository.count();

        DashboardStatsResponse.VideoStats videoStats = DashboardStatsResponse.VideoStats.builder()
                .totalVideos(totalVideos)
                .publishedVideos(publishedVideos)
                .hiddenVideos(hiddenVideos)
                .deletedVideos(deletedVideos)
                .draftVideos(draftVideos)
                .totalViews(totalViews)
                .totalLikes(totalLikes)
                .totalShares(totalShares)
                .totalComments(totalComments)
                .build();

        long totalReports = reportRepository.count();
        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);
        long approvedReports = reportRepository.countByStatus(ReportStatus.APPROVED);
        long rejectedReports = reportRepository.countByStatus(ReportStatus.REJECTED);
        long resolvedReports = reportRepository.countByStatus(ReportStatus.RESOLVED);

        DashboardStatsResponse.ReportStats reportStats = DashboardStatsResponse.ReportStats.builder()
                .totalReports(totalReports)
                .pendingReports(pendingReports)
                .approvedReports(approvedReports)
                .rejectedReports(rejectedReports)
                .resolvedReports(resolvedReports)
                .build();

        long totalCategories = categoryRepository.count();
        long totalHashtags = hashtagRepository.count();

        DashboardStatsResponse.ContentStats contentStats = DashboardStatsResponse.ContentStats.builder()
                .totalCategories(totalCategories)
                .totalHashtags(totalHashtags)
                .build();

        List<Video> topVideosList = videoRepository.findTop10ByOrderByViewsDesc();
        List<VideoResponse> topViewedVideos = topVideosList.stream()
                .map(videoMapper::toVideoResponse)
                .toList();

        List<User> recentUsersList = userRepository.findTop10ByOrderByCreatedAtDesc();
        List<AdminUserResponse> recentUsers = recentUsersList.stream()
                .map(this::mapToAdminUserResponse)
                .toList();

        DashboardStatsResponse dashboard = DashboardStatsResponse.builder()
                .userStats(userStats)
                .videoStats(videoStats)
                .reportStats(reportStats)
                .contentStats(contentStats)
                .topViewedVideos(topViewedVideos)
                .recentUsers(recentUsers)
                .build();

        return ApiResponse.success("Lấy dữ liệu Dashboard thống kê thành công", dashboard);
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .gender(user.getGender())
                .isActive(user.getIsActive())
                .warningCount(user.getWarningCount() != null ? user.getWarningCount() : 0)
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

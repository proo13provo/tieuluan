package com.example.short_video.controller;

import com.example.short_video.Enum.ReportStatus;
import com.example.short_video.Enum.ReportTargetType;
import com.example.short_video.dto.request.*;
import com.example.short_video.dto.response.*;
import com.example.short_video.entity.Category;
import com.example.short_video.entity.Hashtag;
import com.example.short_video.entity.Role;
import com.example.short_video.exception.AuthenticationFailedException;
import com.example.short_video.security.CustomUserDetails;
import com.example.short_video.service.AdminService;
import com.example.short_video.service.ModeratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin", "/api/moderation"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Tag(name = "Quản trị & Kiểm duyệt (Admin & Moderator)", description = "Các chức năng quản trị và kiểm duyệt dùng chung cho cả Admin và Moderator (Video, Comment, Báo cáo, Khóa/Cảnh cáo Người dùng, Thống kê), cùng các chức năng độc quyền của Admin (Phân quyền vai trò, Quản lý danh mục)")
public class AdminController {

    private final AdminService adminService;
    private final ModeratorService moderatorService;


    @GetMapping("/users")
    @Operation(summary = "Danh sách người dùng (Tìm kiếm & Phân trang)", description = "Lấy danh sách người dùng, hỗ trợ tìm kiếm theo username/email và lọc trạng thái hoạt động")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getUsers(keyword, isActive, page, size));
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Khóa hoặc Mở khóa tài khoản người dùng", description = "Thay đổi trạng thái isActive (false = khóa, true = mở khóa)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request, userDetails.getUser()));
    }

    @PostMapping("/users/{userId}/warn")
    @Operation(summary = "Cảnh cáo người dùng vi phạm", description = "Gửi cảnh cáo tới người dùng vi phạm quy định, tăng bộ đếm cảnh cáo")
    public ResponseEntity<ApiResponse<UserWarningResponse>> warnUser(
            @PathVariable Long userId,
            @Valid @RequestBody WarnUserRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(moderatorService.warnUser(userId, request, userDetails.getUser()));
    }

    @GetMapping("/users/{userId}/warnings")
    @Operation(summary = "Xem danh sách cảnh cáo của người dùng", description = "Lấy lịch sử tất cả các lần bị cảnh cáo của một tài khoản")
    public ResponseEntity<ApiResponse<List<UserWarningResponse>>> getUserWarnings(
            @PathVariable Long userId) {
        return ResponseEntity.ok(moderatorService.getUserWarnings(userId));
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Phân quyền / Đổi vai trò người dùng (Admin)", description = "Cập nhật danh sách roles cho tài khoản (USER, MODERATOR, ADMIN)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(adminService.updateUserRoles(userId, request, userDetails.getUser()));
    }

    @PostMapping("/users/{userId}/grant-moderator")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cấp quyền Moderator cho người dùng (Admin)", description = "Admin cấp quyền Kiểm duyệt viên (MODERATOR) cho người dùng chỉ định")
    public ResponseEntity<ApiResponse<AdminUserResponse>> grantModeratorRole(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(adminService.grantModeratorRole(userId, userDetails.getUser()));
    }

    @PostMapping("/users/{userId}/revoke-moderator")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thu hồi quyền Moderator của người dùng (Admin)", description = "Admin thu hồi quyền Kiểm duyệt viên (MODERATOR) của người dùng")
    public ResponseEntity<ApiResponse<AdminUserResponse>> revokeModeratorRole(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(adminService.revokeModeratorRole(userId, userDetails.getUser()));
    }

    @PutMapping("/users/{userId}/moderator")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cấp hoặc thu hồi quyền Moderator (Toggle) (Admin)", description = "Cấp (granted=true) hoặc thu hồi (granted=false) quyền Kiểm duyệt viên cho người dùng")
    public ResponseEntity<ApiResponse<AdminUserResponse>> toggleModeratorRole(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "true") boolean granted,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        if (granted) {
            return ResponseEntity.ok(adminService.grantModeratorRole(userId, userDetails.getUser()));
        } else {
            return ResponseEntity.ok(adminService.revokeModeratorRole(userId, userDetails.getUser()));
        }
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách vai trò hệ thống (Admin)", description = "Lấy tất cả các role hiện có trong hệ thống")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        return ResponseEntity.ok(adminService.getAllRoles());
    }

    @PutMapping("/videos/{videoId}/status")
    @Operation(summary = "Đổi trạng thái video vi phạm (Ẩn / Khôi phục)", description = "Chuyển trạng thái video sang HIDDEN, DELETED hoặc PUBLISHED")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideoStatus(
            @PathVariable Long videoId,
            @Valid @RequestBody UpdateVideoStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(moderatorService.updateVideoStatus(videoId, request, userDetails.getUser()));
    }

    @DeleteMapping("/videos/{videoId}")
    @Operation(summary = "Xóa video vi phạm", description = "Xóa (soft-delete sang DELETED) video vi phạm tiêu chuẩn cộng đồng")
    public ResponseEntity<ApiResponse<Void>> deleteViolatingVideo(
            @PathVariable Long videoId,
            @RequestParam(required = false, defaultValue = "Vi phạm tiêu chuẩn cộng đồng") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(moderatorService.deleteViolatingVideo(videoId, reason, userDetails.getUser()));
    }

    @PutMapping("/videos/{videoId}/comments/toggle")
    @Operation(summary = "Khóa / Mở khóa bình luận trên video", description = "Khóa hoặc mở khóa quyền bình luận đối với video")
    public ResponseEntity<ApiResponse<VideoResponse>> toggleVideoComments(
            @PathVariable Long videoId,
            @RequestParam boolean locked,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(moderatorService.toggleVideoComments(videoId, locked, userDetails.getUser()));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Xóa bình luận vi phạm", description = "Xóa bất kỳ bình luận vi phạm tiêu chuẩn cộng đồng")
    public ResponseEntity<ApiResponse<Void>> deleteViolatingComment(
            @PathVariable Long commentId,
            @RequestParam(required = false, defaultValue = "Nội dung vi phạm tiêu chuẩn cộng đồng") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(moderatorService.deleteViolatingComment(commentId, reason, userDetails.getUser()));
    }

    // ─── 5. XEM VÀ XỬ LÝ BÁO CÁO (DÙNG CHUNG ADMIN & MODERATOR) ───────────────

    @GetMapping("/reports")
    @Operation(summary = "Xem danh sách báo cáo vi phạm", description = "Danh sách báo cáo phân trang, lọc theo status (PENDING, APPROVED, REJECTED, RESOLVED) và targetType (VIDEO, COMMENT, USER)")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(moderatorService.getReports(status, targetType, page, size));
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Xem chi tiết một báo cáo vi phạm", description = "Lấy đầy đủ thông tin báo cáo kèm thông tin người báo cáo và người vi phạm")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @PathVariable Long reportId) {
        return ResponseEntity.ok(moderatorService.getReportById(reportId));
    }

    @PutMapping("/reports/{reportId}/process")
    @Operation(summary = "Xử lý báo cáo vi phạm", description = "Phê duyệt/Từ chối báo cáo và tự động thực thi hành động: HIDE_VIDEO, DELETE_VIDEO, LOCK_COMMENTS, DELETE_COMMENT, WARN_USER, BAN_USER, DISMISSED")
    public ResponseEntity<ApiResponse<ReportResponse>> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ProcessReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return ResponseEntity.ok(moderatorService.processReport(reportId, request, userDetails.getUser()));
    }

    // ─── 6. DASHBOARD VÀ THỐNG KÊ (DÙNG CHUNG ADMIN & MODERATOR) ──────────────

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Thống kê hệ thống và Dashboard", description = "Tổng quan số liệu người dùng, video, báo cáo, danh mục, top video và người dùng mới")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ─── 7. QUẢN LÝ DANH MỤC (CATEGORY) ───────────────────────────────────────

    @GetMapping("/categories")
    @Operation(summary = "Danh sách danh mục video", description = "Lấy tất cả danh mục video (Admin & Mod)")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        return ResponseEntity.ok(adminService.getAllCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo danh mục mới (Admin)", description = "Thêm mới một danh mục phân loại video")
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật danh mục (Admin)", description = "Chỉnh sửa tên danh mục")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(adminService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa danh mục (Admin)", description = "Xóa danh mục khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteCategory(id));
    }

    @GetMapping("/hashtags")
    @Operation(summary = "Danh sách hashtag", description = "Lấy danh sách hashtag phân trang, tìm kiếm theo tên và sắp xếp theo lượt sử dụng (Admin & Mod)")
    public ResponseEntity<ApiResponse<Page<Hashtag>>> getAllHashtags(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllHashtags(keyword, page, size));
    }

    @PutMapping("/hashtags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chỉnh sửa hashtag (Admin)", description = "Cập nhật tên hashtag")
    public ResponseEntity<ApiResponse<Hashtag>> updateHashtag(
            @PathVariable Long id,
            @Valid @RequestBody HashtagRequest request) {
        return ResponseEntity.ok(adminService.updateHashtag(id, request));
    }

    @DeleteMapping("/hashtags/{id}")
    @Operation(summary = "Xóa hashtag vi phạm", description = "Xóa hashtag rác hoặc vi phạm khỏi hệ thống (Admin & Mod)")
    public ResponseEntity<ApiResponse<Void>> deleteHashtag(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteHashtag(id));
    }

    private void requireAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("Vui lòng đăng nhập với quyền Quản trị viên hoặc Kiểm duyệt viên");
        }
    }
}

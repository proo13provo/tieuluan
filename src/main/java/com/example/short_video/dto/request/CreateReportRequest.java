package com.example.short_video.dto.request;

import com.example.short_video.Enum.ReportReason;
import com.example.short_video.Enum.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "Loại đối tượng báo cáo không được để trống (VIDEO, COMMENT, USER)")
    private ReportTargetType targetType;

    @NotNull(message = "ID của đối tượng bị báo cáo không được để trống")
    private Long targetId;

    @NotNull(message = "Lý do báo cáo không được để trống")
    private ReportReason reason;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
}

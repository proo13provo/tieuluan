package com.example.short_video.dto.request;

import com.example.short_video.Enum.ReportStatus;
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
public class ProcessReportRequest {

    @NotNull(message = "Trạng thái xử lý không được để trống (APPROVED, REJECTED, RESOLVED)")
    private ReportStatus status;

    @Size(max = 1000, message = "Ghi chú xử lý không được vượt quá 1000 ký tự")
    private String resolutionNote;
    @Size(max = 100, message = "Hành động xử lý không được vượt quá 100 ký tự")
    private String actionTaken;
}

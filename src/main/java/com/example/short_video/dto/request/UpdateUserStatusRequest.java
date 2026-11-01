package com.example.short_video.dto.request;

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
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống (true = mở khóa, false = khóa)")
    private Boolean isActive;

    @Size(max = 500, message = "Lý do khóa/mở khóa không được vượt quá 500 ký tự")
    private String reason;
}

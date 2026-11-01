package com.example.short_video.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchFollowStatusRequest {

    @NotEmpty(message = "Danh sách targetUserIds không được để trống")
    private List<Long> targetUserIds;
}

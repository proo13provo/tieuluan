package com.example.short_video.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request ghi nhận lịch sử xem video")
public class RecordWatchHistoryRequest {

    @PositiveOrZero(message = "Thời lượng đã xem phải lớn hơn hoặc bằng 0")
    @Schema(description = "Thời lượng video đã xem (tính bằng giây)", example = "15.5")
    private Double watchedDuration;

    @Schema(description = "Người dùng đã xem hết video hay chưa", example = "true")
    private Boolean completed;
}

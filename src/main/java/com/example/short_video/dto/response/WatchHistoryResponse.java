package com.example.short_video.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin bản ghi lịch sử xem/nghe video")
public class WatchHistoryResponse {

    @Schema(description = "ID của bản ghi lịch sử", example = "1")
    private Long id;

    @Schema(description = "ID của video", example = "10")
    private Long videoId;

    @Schema(description = "Chi tiết thông tin video đã xem/nghe")
    private VideoResponse video;

    @Schema(description = "Thời lượng đã xem (giây)", example = "45.0")
    private Double watchedDuration;

    @Schema(description = "Đã xem hết video hay chưa", example = "true")
    private Boolean completed;

    @Schema(description = "Số lần đã xem/nghe video này", example = "3")
    private Integer watchCount;

    @Schema(description = "Thời điểm xem/nghe gần nhất")
    private Instant lastWatchedAt;

    @Schema(description = "Thời điểm lần đầu xem/nghe video")
    private Instant createdAt;
}

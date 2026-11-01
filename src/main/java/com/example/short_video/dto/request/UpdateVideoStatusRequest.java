package com.example.short_video.dto.request;

import com.example.short_video.Enum.VideoStatus;
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
public class UpdateVideoStatusRequest {

    @NotNull(message = "Trạng thái video không được để trống (HIDDEN, DELETED, PUBLISHED)")
    private VideoStatus status;

    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;
}

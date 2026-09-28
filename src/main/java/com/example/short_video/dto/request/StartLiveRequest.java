package com.example.short_video.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StartLiveRequest {

    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;
}

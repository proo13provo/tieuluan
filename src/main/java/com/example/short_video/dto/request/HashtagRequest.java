package com.example.short_video.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagRequest {

    @NotBlank(message = "Tên hashtag không được để trống")
    @Size(min = 1, max = 100, message = "Tên hashtag phải từ 1 đến 100 ký tự")
    private String name;
}

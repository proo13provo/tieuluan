package com.example.short_video.dto.response;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {
    private String publicId;

    private String url;

    private String format;

    private Long bytes;

    private Integer width;

    private Integer height;

    private Double duration;

    private String resourceType;
}

package com.example.short_video.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishVideoRequest {
    private String title;
    private List<String> hashtags;
    private Boolean isComment;
}

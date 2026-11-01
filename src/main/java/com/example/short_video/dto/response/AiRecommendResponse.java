package com.example.short_video.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecommendResponse {
    private Long userId;
    private String strategy;
    private int page;
    private int limit;
    private int total;
    private List<Map<String, Object>> videos;
}

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
public class AiSearchResponse {
    private String query;
    private String strategy;
    private String sortBy;
    private int page;
    private int limit;
    private int totalUsers;
    private int totalVideos;
    private Map<String, Object> modelInfo;
    private List<Map<String, Object>> users;
    private List<Map<String, Object>> videos;
}

package com.example.short_video.service;

import com.example.short_video.dto.request.AiRecommendRequest;
import com.example.short_video.dto.response.AiRecommendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
@Slf4j
public class AiRecommendationClient {

    private final RestClient restClient;
    private final String aiRecommenderUrl;
    private final String aiSearchUrl;

    public AiRecommendationClient(
            @Value("${ai.recommender.url:http://localhost:5000/api/recommend}") String aiRecommenderUrl,
            @Value("${ai.search.url:}") String aiSearchUrl
    ) {
        this.aiRecommenderUrl = aiRecommenderUrl;
        if (aiSearchUrl != null && !aiSearchUrl.isBlank()) {
            this.aiSearchUrl = aiSearchUrl;
        } else {
            this.aiSearchUrl = aiRecommenderUrl.contains("/api/recommend")
                    ? aiRecommenderUrl.replace("/api/recommend", "/api/search")
                    : "http://localhost:5000/api/search";
        }
        this.restClient = RestClient.builder().build();
    }

    public AiRecommendResponse getRecommendations(AiRecommendRequest request) {
        try {
            log.info("Gọi sang AI Recommender microservice tại: {}", aiRecommenderUrl);
            return restClient.post()
                    .uri(aiRecommenderUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiRecommendResponse.class);
        } catch (Exception e) {
            log.warn("Không thể kết nối đến AI Recommender Service ({}). Sẽ sử dụng Fallback Ranking nội bộ. Lỗi: {}",
                    aiRecommenderUrl, e.getMessage());
            return null;
        }
    }
    
    public com.example.short_video.dto.response.AiSearchResponse search(com.example.short_video.dto.request.AiSearchRequest request) {
        try {
            log.info("Gọi sang AI Unified Smart Search microservice tại: {}", aiSearchUrl);
            return restClient.post()
                    .uri(aiSearchUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(com.example.short_video.dto.response.AiSearchResponse.class);
        } catch (Exception e) {
            log.warn("Không thể kết nối đến AI Smart Search Service ({}). Sẽ sử dụng Fallback Database Search. Lỗi: {}",
                    aiSearchUrl, e.getMessage());
            return null;
        }
    }
}

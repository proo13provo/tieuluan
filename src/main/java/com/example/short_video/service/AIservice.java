package com.example.short_video.service;

import com.example.short_video.entity.Category;
import com.example.short_video.repository.CategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIservice {

    private final ChatClient chatClient;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    public Set<Category> categorizeVideo(String title, List<String> hashtags) {
        Set<Category> matchedCategories = new HashSet<>();

        if ((title == null || title.isBlank()) && (hashtags == null || hashtags.isEmpty())) {
            matchedCategories.add(getOrCreateCategory(com.example.short_video.Enum.Category.OTHER.name()));
            return matchedCategories;
        }

        try {
            String availableCategories = Arrays.stream(com.example.short_video.Enum.Category.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            String hashtagsStr = (hashtags != null && !hashtags.isEmpty()) ? String.join(", ", hashtags) : "Không có";

            String prompt = """
                    Phân loại video vào 1+ thể loại từ: [%s]
                    Chỉ dùng đúng tên thể loại trong danh sách.
                    Không rõ → ["OTHER"].
                    Chỉ trả JSON array.
                    
                    Tiêu đề: "%s"
                    Hashtags: %s
                    """.formatted(availableCategories, title != null ? title : "", hashtagsStr);

            String responseContent = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            List<String> predictedCategoryNames = parseCategoriesResponse(responseContent);

            for (String catName : predictedCategoryNames) {
                String cleanName = catName.trim().toUpperCase();
                try {
                    com.example.short_video.Enum.Category enumCat = com.example.short_video.Enum.Category.valueOf(cleanName);
                    Category category = getOrCreateCategory(enumCat.name());
                    matchedCategories.add(category);
                } catch (IllegalArgumentException ex) {
                    log.warn("Category trả về từ Spring AI không hợp lệ: {}", cleanName);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Spring AI để phân loại video: {}", e.getMessage(), e);
            return fallbackCategorize(title, hashtags);
        }

        if (matchedCategories.isEmpty()) {
            matchedCategories.add(getOrCreateCategory(com.example.short_video.Enum.Category.OTHER.name()));
        }

        return matchedCategories;
    }

    private List<String> parseCategoriesResponse(String responseText) {
        List<String> categories = new ArrayList<>();
        if (responseText == null || responseText.isBlank()) {
            return categories;
        }
        try {
            String rawText = responseText.trim();
            if (rawText.startsWith("```json")) {
                rawText = rawText.substring(7);
            } else if (rawText.startsWith("```")) {
                rawText = rawText.substring(3);
            }
            if (rawText.endsWith("```")) {
                rawText = rawText.substring(0, rawText.length() - 3);
            }
            rawText = rawText.trim();

            categories = objectMapper.readValue(rawText, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Lỗi parse phản hồi JSON từ Spring AI: {}", e.getMessage());
        }
        return categories;
    }

    private Set<Category> fallbackCategorize(String title, List<String> hashtags) {
        Set<Category> result = new HashSet<>();
        String combined = ((title != null ? title : "") + " " + (hashtags != null ? String.join(" ", hashtags) : "")).toLowerCase();

        for (com.example.short_video.Enum.Category catEnum : com.example.short_video.Enum.Category.values()) {
            String catName = catEnum.name().toLowerCase();
            if (combined.contains(catName)) {
                result.add(getOrCreateCategory(catEnum.name()));
            }
        }

        if (result.isEmpty()) {
            result.add(getOrCreateCategory(com.example.short_video.Enum.Category.OTHER.name()));
        }

        return result;
    }

    private Category getOrCreateCategory(String name) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).build()));
    }
}

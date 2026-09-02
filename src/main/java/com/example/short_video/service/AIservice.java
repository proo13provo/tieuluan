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

    /**
     * Phân tích title và hashtags bằng Spring AI ChatClient để gán categories cho video.
     * 1 video có thể có nhiều categories.
     */
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
                    Bạn là hệ thống AI phân loại nội dung video ngắn.
                    Hãy phân tích tiêu đề và danh sách hashtag sau đây, rồi chọn 1 hoặc NHIỀU thể loại phù hợp nhất từ danh sách thể loại cho trước.
                    
                    Danh sách thể loại hợp lệ:
                    [%s]
                    
                    Quy tắc:
                    1. Một video có thể thuộc nhiều thể loại (ví dụ video hướng dẫn lập trình có thể thuộc PROGRAMMING, TECHNOLOGY và EDUCATION).
                    2. CHỈ ĐƯỢC CHỌN các thể loại có trong danh sách trên, tên viết hoa chính xác.
                    3. Trả về định dạng JSON array chứa tên các thể loại đã chọn, ví dụ: ["PROGRAMMING", "TECHNOLOGY"]
                    4. Nếu không rõ hoặc không thuộc thể loại cụ thể nào, trả về: ["OTHER"]
                    
                    Thông tin video:
                    - Tiêu đề: "%s"
                    - Hashtags: %s
                    
                    Chỉ trả về duy nhất mảng JSON, không kèm bất kỳ văn bản giải thích nào khác.
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

package com.example.short_video.mapper;

import com.example.short_video.dto.response.VideoResponse;
import com.example.short_video.dto.response.VideoUploadResponse;
import com.example.short_video.entity.Category;
import com.example.short_video.entity.Video;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = HashtagMapper.class)
public interface VideoMapper {


    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "hashtags", source = "hashtags")
    @Mapping(target = "categories", source = "categories", qualifiedByName = "categoriesToNames")
    @Mapping(target = "status", expression = "java(video.getStatus() != null ? video.getStatus().name() : null)")
    @Mapping(target = "liked", ignore = true)
    VideoResponse toVideoResponse(Video video);


    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "hashtags", source = "hashtags")
    @Mapping(target = "categories", source = "categories", qualifiedByName = "categoriesToNames")
    @Mapping(target = "status", expression = "java(video.getStatus() != null ? video.getStatus().name() : null)")
    VideoUploadResponse toVideoUploadResponse(Video video);


    @Named("categoriesToNames")
    default List<String> categoriesToNames(Set<Category> categories) {
        if (categories == null) return Collections.emptyList();
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.toList());
    }
}

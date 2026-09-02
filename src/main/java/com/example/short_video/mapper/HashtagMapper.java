package com.example.short_video.mapper;

import com.example.short_video.dto.response.HashtagDto;
import com.example.short_video.entity.Hashtag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HashtagMapper {

    HashtagDto toDto(Hashtag hashtag);

    List<HashtagDto> toDtoList(List<Hashtag> hashtags);
}

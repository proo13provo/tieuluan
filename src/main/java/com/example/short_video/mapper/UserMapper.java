package com.example.short_video.mapper;

import com.example.short_video.dto.response.UserProfileResponse;
import com.example.short_video.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "followerCount", source = "followerCount")
    @Mapping(target = "followingCount", source = "followingCount")
    @Mapping(target = "totalLikes", source = "totalLikes")
    @Mapping(target = "isFollowing", ignore = true)
    @Mapping(target = "isFollowedBy", ignore = true)
    @Mapping(target = "isMutual", ignore = true)
    UserProfileResponse toUserProfileResponse(User user, long followerCount, long followingCount, long totalLikes);
}

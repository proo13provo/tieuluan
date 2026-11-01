package com.example.short_video.dto.response;

import com.example.short_video.Enum.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
    private Gender gender;
    private Boolean isActive;
    private Integer warningCount;
    private Set<String> roles;
    private Date createdAt;
}

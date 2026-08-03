package com.example.short_video.entity;

import com.example.short_video.Enum.Gender;

import java.util.*;

public class User {
    private long id;
    private String username;
    private String email;
    private String password;
    private String avatarUrl;
    private String bio;
    private Gender gender;
    private String location;
    private Boolean isActive;
    private Date createdAt;
}

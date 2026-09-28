package com.example.short_video.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "zego")
@Data
public class ZegoProperties {
    private long appId = 280768762L;
    private String appSign = "c628b1e73dfbc0e5466c32f2b2fece03758a78b55bf2be50e2330f3248b3677e";
    private String serverSecret = "6c7ec2e8e9956fd60f2b2b14e0fc42f3";
}

package com.example.busbooking.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        String projectId,
        String serviceAccountPath,
        String webApiKey
) {
}

package com.example.taskmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin; // Injects the allowed origin from application.properties

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // This is the correct, conflict-free CORS configuration.
        // The allowedOrigins is injected, and the conflicting allowCredentials(true) is removed.
        registry.addMapping("/**") // Apply to all API endpoints
                .allowedOrigins(allowedOrigin) 
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*"); 
    }
}
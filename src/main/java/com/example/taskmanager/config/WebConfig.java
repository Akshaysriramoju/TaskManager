package com.example.taskmanager.config; // NOTE: Adjust package name if necessary

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // You must allow the specific host and port that the client (Nginx) is serving from.
        // Nginx is serving from the public IP on the standard HTTP port (80).
        String allowedOrigin = "http://65.2.36.235";

        registry.addMapping("/**") // Apply to all API endpoints
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
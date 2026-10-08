package com.happynest.pg.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Comma-separated origins/patterns from env APP_CORS_ALLOWED_ORIGINS.
     * Patterns like https://*.vercel.app are supported.
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,https://*.vercel.app}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> patterns = new ArrayList<>(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        // Always keep local + Vercel preview/prod working even if env is mis-set
        if (!patterns.contains("http://localhost:5173")) {
            patterns.add("http://localhost:5173");
        }
        if (!patterns.contains("https://*.vercel.app")) {
            patterns.add("https://*.vercel.app");
        }
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

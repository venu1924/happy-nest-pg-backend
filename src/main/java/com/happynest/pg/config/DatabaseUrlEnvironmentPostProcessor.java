package com.happynest.pg.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts Render/Railway DATABASE_URL (postgres://…) into Spring JDBC properties
 * before the app starts. Fixes deploy crash when localhost defaults would be used.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                System.getenv("DATABASE_URL"),
                environment.getProperty("database.url")
        );
        boolean prod = environment.matchesProfiles("prod")
                || "prod".equalsIgnoreCase(System.getenv("SPRING_PROFILES_ACTIVE"));
        if (databaseUrl == null || databaseUrl.isBlank()) {
            if (prod) {
                throw new IllegalStateException(
                        "DATABASE_URL is missing. On Render: Web Service → Environment → "
                                + "Add DATABASE_URL = Internal Database URL from happy-nest-db.");
            }
            return;
        }
        if (!(databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            return;
        }

        try {
            URI uri = URI.create(databaseUrl);
            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                username = decode(parts[0]);
                password = parts.length > 1 ? decode(parts[1]) : "";
            }

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() != null ? uri.getPath() : "/happy_nest";
            String jdbc = "jdbc:postgresql://" + host + ":" + port + path;
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbc = jdbc + "?" + uri.getQuery();
            } else {
                jdbc = jdbc + "?sslmode=require";
            }

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbc);
            if (username != null) {
                props.put("spring.datasource.username", username);
            }
            if (password != null) {
                props.put("spring.datasource.password", password);
            }
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("server.port", firstNonBlank(System.getenv("PORT"), environment.getProperty("PORT"), "8080"));

            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlOverride", props));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse DATABASE_URL for JDBC", e);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}

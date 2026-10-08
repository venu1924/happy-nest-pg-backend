package com.happynest.pg.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

/**
 * When DATABASE_URL is set (Render/Railway), convert postgres://… to JDBC.
 * Local runs keep using application.properties.
 */
@Configuration
public class DatabaseUrlConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties(Environment env) {
        DataSourceProperties props = new DataSourceProperties();
        String databaseUrl = env.getProperty("DATABASE_URL");
        try {
            URI uri = URI.create(databaseUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                props.setUsername(parts[0]);
                props.setPassword(parts.length > 1 ? parts[1] : "");
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
            props.setUrl(jdbc);
            props.setDriverClassName("org.postgresql.Driver");
        } catch (Exception e) {
            throw new IllegalStateException("Invalid DATABASE_URL", e);
        }
        return props;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}

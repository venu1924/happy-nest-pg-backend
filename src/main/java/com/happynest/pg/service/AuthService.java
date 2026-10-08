package com.happynest.pg.service;

import com.happynest.pg.dto.AdminDto;
import com.happynest.pg.dto.LoginRequest;
import com.happynest.pg.dto.LoginResponse;
import com.happynest.pg.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Fixed username/password login for Phase 2 (simple token — not full Spring Security).
 */
@Service
public class AuthService {

    @Value("${app.auth.username:admin}")
    private String username;

    @Value("${app.auth.password:happynest@602}")
    private String password;

    @Value("${app.auth.admin-name:Admin}")
    private String adminName;

    private static final String TOKEN_PREFIX = "hn.";

    public LoginResponse login(LoginRequest request) {
        String u = request.getUsername() != null ? request.getUsername().trim() : "";
        String p = request.getPassword() != null ? request.getPassword() : "";

        if (!username.equals(u) || !password.equals(p)) {
            throw new ApiException("Invalid username or password.", HttpStatus.UNAUTHORIZED);
        }

        String raw = username + ":" + UUID.randomUUID();
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));

        return LoginResponse.builder()
                .token(token)
                .admin(AdminDto.builder()
                        .id("admin-1")
                        .name(adminName)
                        .role("Administrator")
                        .build())
                .build();
    }

    public boolean isValidToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring(7).trim();
        if (!token.startsWith(TOKEN_PREFIX)) {
            return false;
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(token.substring(TOKEN_PREFIX.length())),
                    StandardCharsets.UTF_8);
            return decoded.startsWith(username + ":");
        } catch (Exception e) {
            return false;
        }
    }

    public AdminDto currentAdmin() {
        return AdminDto.builder()
                .id("admin-1")
                .name(adminName)
                .role("Administrator")
                .build();
    }
}

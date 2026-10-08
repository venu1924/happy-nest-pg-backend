package com.happynest.pg.controller;

import com.happynest.pg.dto.AdminDto;
import com.happynest.pg.dto.DashboardDto;
import com.happynest.pg.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "ok");
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/admin/me")
    public AdminDto adminMe() {
        return dashboardService.getCurrentAdmin();
    }
}

package com.labourse.admin.controller;

import com.labourse.admin.dto.AdminDtos.DashboardResponse;
import com.labourse.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public DashboardResponse summary() {
        return dashboardService.summary();
    }
}

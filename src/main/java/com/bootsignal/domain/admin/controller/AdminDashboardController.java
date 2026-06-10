package com.bootsignal.domain.admin.controller;

import com.bootsignal.domain.admin.dto.AdminDashboardSummaryResponse;
import com.bootsignal.domain.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public AdminDashboardSummaryResponse getSummary() {
        return adminDashboardService.getSummary();
    }
}

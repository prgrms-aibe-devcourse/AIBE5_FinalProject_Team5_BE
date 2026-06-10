package com.bootsignal.domain.report.controller;

import com.bootsignal.domain.report.dto.AdminReportProcessRequest;
import com.bootsignal.domain.report.dto.AdminReportResponse;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import com.bootsignal.domain.report.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public Page<AdminReportResponse> getList(
        @RequestParam(required = false) ReportStatus status,
        @RequestParam(required = false) ReportTargetType targetType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminReportService.getList(
            status,
            targetType,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{reportId}")
    public AdminReportResponse get(@PathVariable Long reportId) {
        return adminReportService.get(reportId);
    }

    @PatchMapping("/{reportId}")
    public AdminReportResponse process(
        @PathVariable Long reportId,
        @RequestBody @Valid AdminReportProcessRequest request
    ) {
        return adminReportService.process(reportId, request);
    }
}

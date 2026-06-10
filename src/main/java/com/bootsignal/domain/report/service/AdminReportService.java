package com.bootsignal.domain.report.service;

import com.bootsignal.domain.report.dto.AdminReportProcessRequest;
import com.bootsignal.domain.report.dto.AdminReportResponse;
import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import com.bootsignal.domain.report.repository.ReportRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final ReportRepository reportRepository;

    public Page<AdminReportResponse> getList(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        return reportRepository.findByFilters(status, targetType, pageable)
            .map(AdminReportResponse::from);
    }

    public AdminReportResponse get(Long reportId) {
        return AdminReportResponse.from(findReport(reportId));
    }

    @Transactional
    public AdminReportResponse process(Long reportId, AdminReportProcessRequest request) {
        Report report = findReport(reportId);
        report.process(request.status(), request.action(), request.reason());
        return AdminReportResponse.from(report);
    }

    private Report findReport(Long reportId) {
        return reportRepository.findById(reportId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.REPORT_NOT_FOUND));
    }
}

package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import java.time.LocalDateTime;

public record AdminReportResponse(
    Long reportId,
    Long reporterId,
    String reporterNickname,
    ReportTargetType targetType,
    Long targetId,
    String reason,
    String detail,
    ReportStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
            report.getId(),
            report.getReporter().getId(),
            report.getReporter().getNickname(),
            report.getTargetType(),
            report.getTargetId(),
            report.getReason(),
            report.getDetail(),
            report.getStatus(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
}

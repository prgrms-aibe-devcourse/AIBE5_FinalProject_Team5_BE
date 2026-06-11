package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import java.time.LocalDateTime;

public record ReportResponse(
    Long reportId,
    ReportTargetType targetType,
    Long targetId,
    ReportStatus status,
    LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
            report.getId(),
            report.getTargetType(),
            report.getTargetId(),
            report.getStatus(),
            report.getCreatedAt()
        );
    }
}

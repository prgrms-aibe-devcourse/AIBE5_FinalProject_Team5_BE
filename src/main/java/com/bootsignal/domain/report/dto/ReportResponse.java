package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import java.time.LocalDateTime;

/**
 * 신고 등록 후 프론트에 신고 식별자와 대기 상태를 반환하는 응답 DTO입니다.
 */
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

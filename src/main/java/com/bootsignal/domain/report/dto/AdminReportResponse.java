package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.Report;
import com.bootsignal.domain.report.entity.ReportAction;
import com.bootsignal.domain.report.entity.ReportStatus;
import com.bootsignal.domain.report.entity.ReportTargetType;
import java.time.LocalDateTime;

/**
 * 관리자 신고 목록/상세 화면에 필요한 신고자, 대상 콘텐츠, 처리 상태 정보를 담는 응답 DTO입니다.
 */
public record AdminReportResponse(
    Long id,
    Long reportId,
    Long reporterId,
    String reporterName,
    String reporterNickname,
    String profileImageUrl,
    String reportedAt,
    ReportTargetType type,
    ReportTargetType targetType,
    Long targetId,
    String targetLabel,
    String reasonCategory,
    String reasonDetail,
    String reason,
    String detail,
    String contentBody,
    String contentUrl,
    ReportStatus status,
    ReportAction contentAction,
    ReportAction action,
    String processReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AdminReportResponse from(Report report, ReportTargetSnapshot targetSnapshot) {
        return new AdminReportResponse(
            report.getId(),
            report.getId(),
            report.getReporter().getId(),
            report.getReporter().getNickname(),
            report.getReporter().getNickname(),
            report.getReporter().getProfileImageUrl(),
            toReportedAt(report),
            report.getTargetType(),
            report.getTargetType(),
            report.getTargetId(),
            targetSnapshot.targetLabel(),
            report.getReason(),
            report.getDetail(),
            report.getReason(),
            report.getDetail(),
            targetSnapshot.contentBody(),
            targetSnapshot.contentUrl(),
            report.getStatus(),
            report.getAction(),
            report.getAction(),
            report.getProcessReason(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }

    private static String toReportedAt(Report report) {
        return report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate().toString() : null;
    }
}

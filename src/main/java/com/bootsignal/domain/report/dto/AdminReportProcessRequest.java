package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.ReportAction;
import com.bootsignal.domain.report.entity.ReportStatus;
import jakarta.validation.constraints.AssertTrue;

/**
 * 관리자 신고 처리 API에서 처리 상태와 콘텐츠 조치를 받는 요청 DTO입니다.
 */
public record AdminReportProcessRequest(
    ReportStatus status,
    ReportAction action,
    ReportAction contentAction,
    String reason
) {

    @AssertTrue(message = "신고 처리 조치는 필수입니다.")
    public boolean isProcessActionPresent() {
        return action != null || contentAction != null;
    }

    public ReportStatus resolvedStatus() {
        return status != null ? status : ReportStatus.COMPLETED;
    }

    public ReportAction resolvedAction() {
        return contentAction != null ? contentAction : action;
    }
}

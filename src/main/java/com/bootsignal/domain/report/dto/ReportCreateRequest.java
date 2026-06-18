package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 신고 등록 API에서 대상 유형, 대상 ID, 신고 사유와 상세 내용을 받는 요청 DTO입니다.
 */
public record ReportCreateRequest(
    @NotNull ReportTargetType targetType,
    @NotNull Long targetId,
    @NotBlank String reason,
    @NotBlank String detail
) {}

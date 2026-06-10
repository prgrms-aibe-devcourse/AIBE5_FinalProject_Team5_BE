package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
    @NotNull ReportTargetType targetType,
    @NotNull Long targetId,
    @NotBlank String reason,
    @NotBlank String detail
) {}

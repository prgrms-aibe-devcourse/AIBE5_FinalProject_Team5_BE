package com.bootsignal.domain.report.dto;

import com.bootsignal.domain.report.entity.ReportAction;
import com.bootsignal.domain.report.entity.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminReportProcessRequest(
    @NotNull ReportStatus status,
    @NotNull ReportAction action,
    @NotBlank String reason
) {}

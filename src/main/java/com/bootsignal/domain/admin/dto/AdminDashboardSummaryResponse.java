package com.bootsignal.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminDashboardSummaryResponse(
    long userCount,
    long courseCount,
    long pendingVerificationCount,
    long reviewCount,
    long reportCount,
    LocalDateTime lastHrdCollectedAt,
    LocalDateTime lastHrdRefinedAt
) {}

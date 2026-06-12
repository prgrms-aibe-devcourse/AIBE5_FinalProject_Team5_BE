package com.bootsignal.domain.work24.dto;

import java.time.LocalDateTime;

public record ReviewCrawlResult(
        String externalReviewId,
        String reviewerNickname,
        Integer rating,
        String content,
        LocalDateTime reviewedAt
) {
}

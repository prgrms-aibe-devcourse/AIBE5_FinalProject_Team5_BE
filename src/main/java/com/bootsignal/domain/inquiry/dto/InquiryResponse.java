package com.bootsignal.domain.inquiry.dto;

import com.bootsignal.domain.inquiry.entity.Inquiry;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 사용자 문의 목록과 상세 화면에 필요한 문의 응답 DTO입니다.
 */
public record InquiryResponse(
    Long id,
    Long inquiryId,
    String title,
    String requestedAt,
    InquiryStatus status,
    String content,
    String adminReply,
    LocalDateTime answeredAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    private static final DateTimeFormatter REQUESTED_AT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public InquiryResponse(
        Long inquiryId,
        String title,
        String content,
        InquiryStatus status,
        String adminReply,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this(
            inquiryId,
            inquiryId,
            title,
            formatRequestedAt(createdAt),
            status,
            content,
            adminReply,
            answeredAt,
            createdAt,
            updatedAt
        );
    }

    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
            inquiry.getId(),
            inquiry.getId(),
            inquiry.getTitle(),
            formatRequestedAt(inquiry.getCreatedAt()),
            inquiry.getStatus(),
            inquiry.getContent(),
            inquiry.getAdminReply(),
            inquiry.getAnsweredAt(),
            inquiry.getCreatedAt(),
            inquiry.getUpdatedAt()
        );
    }

    private static String formatRequestedAt(LocalDateTime requestedAt) {
        return requestedAt != null ? requestedAt.format(REQUESTED_AT_FORMATTER) : null;
    }
}

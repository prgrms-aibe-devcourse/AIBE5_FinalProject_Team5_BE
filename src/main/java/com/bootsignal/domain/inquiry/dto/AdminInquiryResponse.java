package com.bootsignal.domain.inquiry.dto;

import com.bootsignal.domain.inquiry.entity.Inquiry;
import com.bootsignal.domain.inquiry.entity.InquiryStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 관리자 문의 관리 화면에 필요한 작성자 정보와 답변 정보를 포함한 응답 DTO입니다.
 */
public record AdminInquiryResponse(
    Long id,
    Long inquiryId,
    Long userId,
    String userName,
    String userNickname,
    String profileImageUrl,
    String title,
    String requestedAt,
    String content,
    InquiryStatus status,
    String adminReply,
    Long answeredById,
    String answeredByNickname,
    LocalDateTime answeredAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    private static final DateTimeFormatter REQUESTED_AT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public AdminInquiryResponse(
        Long inquiryId,
        Long userId,
        String userName,
        String userNickname,
        String profileImageUrl,
        String title,
        String content,
        InquiryStatus status,
        String adminReply,
        Long answeredById,
        String answeredByNickname,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this(
            inquiryId,
            inquiryId,
            userId,
            userName,
            userNickname,
            profileImageUrl,
            title,
            formatRequestedAt(createdAt),
            content,
            status,
            adminReply,
            answeredById,
            answeredByNickname,
            answeredAt,
            createdAt,
            updatedAt
        );
    }

    public static AdminInquiryResponse from(Inquiry inquiry) {
        return new AdminInquiryResponse(
            inquiry.getId(),
            inquiry.getId(),
            inquiry.getUser().getId(),
            inquiry.getUser().getName(),
            inquiry.getUser().getNickname(),
            inquiry.getUser().getProfileImageUrl(),
            inquiry.getTitle(),
            formatRequestedAt(inquiry.getCreatedAt()),
            inquiry.getContent(),
            inquiry.getStatus(),
            inquiry.getAdminReply(),
            inquiry.getAnsweredBy() != null ? inquiry.getAnsweredBy().getId() : null,
            inquiry.getAnsweredBy() != null ? inquiry.getAnsweredBy().getNickname() : null,
            inquiry.getAnsweredAt(),
            inquiry.getCreatedAt(),
            inquiry.getUpdatedAt()
        );
    }

    private static String formatRequestedAt(LocalDateTime requestedAt) {
        return requestedAt != null ? requestedAt.format(REQUESTED_AT_FORMATTER) : null;
    }
}

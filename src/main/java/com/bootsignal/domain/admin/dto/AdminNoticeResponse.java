package com.bootsignal.domain.admin.dto;

import com.bootsignal.domain.notice.entity.Notice;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 관리자 공지 발송 내역 화면과 공개 공지 연동에 필요한 공지 응답 DTO입니다.
 */
public record AdminNoticeResponse(
    Long id,
    Long noticeId,
    String sentBy,
    String senderNickname,
    String title,
    String content,
    String sentAt,
    LocalDateTime createdAt
) {
    private static final DateTimeFormatter SENT_AT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public AdminNoticeResponse(
        Long noticeId,
        String senderNickname,
        String title,
        String content,
        LocalDateTime createdAt
    ) {
        this(
            noticeId,
            noticeId,
            senderNickname,
            senderNickname,
            title,
            content,
            formatSentAt(createdAt),
            createdAt
        );
    }

    public static AdminNoticeResponse from(Notice notice) {
        return new AdminNoticeResponse(
            notice.getId(),
            notice.getId(),
            notice.getSender().getNickname(),
            notice.getSender().getNickname(),
            notice.getTitle(),
            notice.getContent(),
            formatSentAt(notice.getCreatedAt()),
            notice.getCreatedAt()
        );
    }

    private static String formatSentAt(LocalDateTime sentAt) {
        return sentAt != null ? sentAt.format(SENT_AT_FORMATTER) : null;
    }
}

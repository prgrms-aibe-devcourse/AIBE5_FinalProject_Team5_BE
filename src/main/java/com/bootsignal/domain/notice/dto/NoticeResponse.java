package com.bootsignal.domain.notice.dto;

import com.bootsignal.domain.notice.entity.Notice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 고객센터 공개 공지 목록과 상세 화면에 필요한 공지 응답 DTO입니다.
 */
public record NoticeResponse(
    Long id,
    Long noticeId,
    String title,
    String content,
    String postedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    private static final DateTimeFormatter POSTED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public NoticeResponse(
        Long noticeId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this(
            noticeId,
            noticeId,
            title,
            content,
            formatPostedAt(createdAt),
            createdAt,
            updatedAt
        );
    }

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
            notice.getId(),
            notice.getId(),
            notice.getTitle(),
            notice.getContent(),
            formatPostedAt(notice.getCreatedAt()),
            notice.getCreatedAt(),
            notice.getUpdatedAt()
        );
    }

    private static String formatPostedAt(LocalDateTime postedAt) {
        return postedAt != null ? postedAt.format(POSTED_AT_FORMATTER) : null;
    }
}

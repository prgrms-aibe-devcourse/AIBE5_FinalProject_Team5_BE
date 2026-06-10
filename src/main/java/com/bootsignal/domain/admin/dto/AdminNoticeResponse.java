package com.bootsignal.domain.admin.dto;

import com.bootsignal.domain.notice.entity.Notice;
import java.time.LocalDateTime;

public record AdminNoticeResponse(
    Long noticeId,
    String senderNickname,
    String title,
    String content,
    LocalDateTime createdAt
) {
    public static AdminNoticeResponse from(Notice notice) {
        return new AdminNoticeResponse(
            notice.getId(),
            notice.getSender().getNickname(),
            notice.getTitle(),
            notice.getContent(),
            notice.getCreatedAt()
        );
    }
}

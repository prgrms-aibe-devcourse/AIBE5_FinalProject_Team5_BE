package com.bootsignal.domain.bookmark.dto;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import java.time.LocalDateTime;

public record BookmarkCreateResponse(
	Long bookmarkId,
	Long courseSessionId,
	LocalDateTime createdAt
) {
	public static BookmarkCreateResponse from(Bookmark bookmark) {
		return new BookmarkCreateResponse(
			bookmark.getId(),
			bookmark.getCourseSession().getId(),
			bookmark.getCreatedAt()
		);
	}
}

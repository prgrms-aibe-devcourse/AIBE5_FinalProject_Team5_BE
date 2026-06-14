package com.bootsignal.domain.bookmark.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record BookmarkPageResponse<T>(
	List<T> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean last
) {
	public static <T> BookmarkPageResponse<T> from(Page<T> page) {
		return new BookmarkPageResponse<>(
			page.getContent(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.isLast()
		);
	}
}

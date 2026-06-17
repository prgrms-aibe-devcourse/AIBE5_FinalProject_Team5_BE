package com.bootsignal.domain.bookmark.dto;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Arrays;

public enum BookmarkSort {
	LATEST("latest"),
	RATING("rating");

	private final String value;

	BookmarkSort(String value) {
		this.value = value;
	}

	// 정렬 조건 값 반환
	public String value() {
		return value;
	}

	// 정렬 조건 변환
	public static BookmarkSort from(String sort) {
		// 정렬 조건이 없으면 기본값 설정
		if (sort == null || sort.isBlank()) {
			return LATEST;
		}
		// 정렬 조건 값 비교
		return Arrays.stream(values())
			.filter(item -> item.value.equalsIgnoreCase(sort))
			.findFirst()
			.orElseThrow(() -> new BootSignalException(ErrorCode.INVALID_PAGE_PARAMETER));
	}
}

package com.bootsignal.domain.mypage.dto;

import java.util.List;

/**
 * 마이페이지 활동 목록 페이지 응답 DTO입니다.
 *
 * @param items         현재 페이지의 활동 목록
 * @param totalElements 전체 항목 수
 * @param totalPages    전체 페이지 수
 * @param pageNumber    현재 페이지 번호 (0 기반)
 */
public record MyPageActivityResponse(
	List<MyPageItemResponse> items,
	long totalElements,
	int totalPages,
	int pageNumber
) {}

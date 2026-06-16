package com.bootsignal.domain.mypage.dto;

import java.time.LocalDateTime;

/**
 * 마이페이지 활동 목록의 단일 항목 응답 DTO입니다.
 * 게시글, Q&A, 모집글, 댓글, 리뷰를 통합된 형태로 반환합니다.
 *
 * @param id             작성물 ID (게시글/댓글/리뷰 각각의 PK)
 * @param type           작성물 유형 (POST, QNA, RECRUIT, COMMENT, REVIEW)
 * @param titleOrContent 게시글·리뷰는 제목, 댓글은 내용 요약 (최대 100자)
 * @param createdAt      작성 시각
 * @param updatedAt      수정 시각
 * @param deletable      현재 사용자가 삭제할 수 있으면 true (본인 또는 ADMIN)
 */
public record MyPageItemResponse(
	Long id,
	String type,
	String titleOrContent,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Boolean deletable
) {}

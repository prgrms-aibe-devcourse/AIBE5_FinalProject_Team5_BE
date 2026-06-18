package com.bootsignal.domain.mypage.dto;

import java.time.LocalDateTime;

/**
 * 마이페이지 활동 목록의 단일 항목 응답 DTO입니다.
 * 게시글, Q&A, 모집글, 댓글, 리뷰를 통합된 형태로 반환합니다.
 *
 * @param id             작성물 ID (게시글/댓글/리뷰 각각의 PK)
 * @param type           작성물 유형 (BOARD, QNA, PROJECT_RECRUIT, COMMENT, REVIEW)
 * @param titleOrContent 게시글은 제목, 댓글·리뷰는 내용 요약 (최대 100자)
 * @param createdAt      작성 시각
 * @param updatedAt      수정 시각
 * @param postId         댓글이 작성된 게시글 ID (COMMENT 유형인 경우에만 존재)
 * @param courseId       리뷰가 작성된 과정 ID (REVIEW 유형인 경우에만 존재)
 * @param courseSessionId 리뷰가 작성된 과정 회차 ID (REVIEW 유형인 경우에만 존재)
 */
public record MyPageItemResponse(
	Long id,
	String type,
	String titleOrContent,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Long postId,
	Long courseId,
	Long courseSessionId
) {}

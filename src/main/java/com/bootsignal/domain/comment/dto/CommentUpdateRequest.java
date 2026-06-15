package com.bootsignal.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 수정 API에서 전달받는 새 댓글 내용을 검증하는 요청 DTO입니다.
 */
public record CommentUpdateRequest(
	@NotBlank(message = "댓글 내용은 필수입니다.")
	@Size(max = 1000, message = "댓글 내용은 1000자 이하여야 합니다.")
	String content
) {
}

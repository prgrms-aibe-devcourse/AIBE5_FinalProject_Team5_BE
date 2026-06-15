package com.bootsignal.domain.comment.dto;

import com.bootsignal.domain.comment.entity.Comment;
import java.time.LocalDateTime;

/**
 * 댓글 API 응답에 필요한 댓글 식별자, 작성자, 내용과 생성/수정 시간을 담는 DTO입니다.
 */
public record CommentResponse(
	Long commentId,
	Long postId,
	Long userId,
	String userNickname,
	String content,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static CommentResponse from(Comment comment) {
		return new CommentResponse(
			comment.getId(),
			comment.getPost().getId(),
			comment.getUser().getId(),
			comment.getUser().getNickname(),
			comment.getContent(),
			comment.getCreatedAt(),
			comment.getUpdatedAt()
		);
	}
}

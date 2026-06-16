package com.bootsignal.domain.post.dto;

import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.entity.PostType;
import java.time.LocalDateTime;

public record PostResponse(
	Long postId,
	Long userId,
	String userNickname,
	Long courseId,
	PostType postType,

	String title,
	String content,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static PostResponse from(Post post) {
		return new PostResponse(
			post.getId(),
			post.getUser().getId(),
			post.getUser().getNickname(),
			post.getCourse() != null ? post.getCourse().getId() : null,
			post.getPostType(),
	
			post.getTitle(),
			post.getContent(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}

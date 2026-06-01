package com.bootsignal.domain.post.dto;

import com.bootsignal.domain.post.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostCreateRequest(
	Long courseId,
	@NotNull PostType postType,
	String category,
	@NotBlank String title,
	@NotBlank String content
) {}

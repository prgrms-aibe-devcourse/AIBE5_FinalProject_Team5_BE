package com.bootsignal.domain.comment.controller;

import com.bootsignal.domain.comment.dto.CommentCreateRequest;
import com.bootsignal.domain.comment.dto.CommentResponse;
import com.bootsignal.domain.comment.dto.CommentUpdateRequest;
import com.bootsignal.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글별 댓글 목록/작성 API와 댓글 단건 수정/삭제 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@PostMapping("/api/posts/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse create(
		@PathVariable Long postId,
		@RequestBody @Valid CommentCreateRequest request
	) {
		return commentService.create(postId, request);
	}

	@GetMapping("/api/posts/{postId}/comments")
	public Page<CommentResponse> getList(
		@PathVariable Long postId,
		@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
	) {
		return commentService.getList(postId, pageable);
	}

	@PatchMapping("/api/comments/{commentId}")
	public CommentResponse update(
		@PathVariable Long commentId,
		@RequestBody @Valid CommentUpdateRequest request
	) {
		return commentService.update(commentId, request);
	}

	@DeleteMapping("/api/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long commentId) {
		commentService.delete(commentId);
	}
}

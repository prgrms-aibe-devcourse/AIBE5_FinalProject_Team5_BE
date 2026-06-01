package com.bootsignal.domain.post.controller;

import com.bootsignal.domain.post.dto.PostCreateRequest;
import com.bootsignal.domain.post.dto.PostResponse;
import com.bootsignal.domain.post.dto.PostUpdateRequest;
import com.bootsignal.domain.post.entity.PostType;
import com.bootsignal.domain.post.service.PostService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(@RequestBody @Valid PostCreateRequest request) {
		return postService.create(request);
	}

	@GetMapping
	public Page<PostResponse> getList(
		@RequestParam(required = false) PostType postType,
		@RequestParam(required = false) Long courseId,
		@RequestParam(required = false) String keyword,
		@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return postService.getList(postType, courseId, keyword, pageable);
	}

	@GetMapping("/{postId}")
	public PostResponse get(@PathVariable Long postId) {
		return postService.get(postId);
	}

	@PatchMapping("/{postId}")
	public PostResponse update(
		@PathVariable Long postId,
		@RequestBody @Valid PostUpdateRequest request
	) {
		return postService.update(postId, request);
	}

	@DeleteMapping("/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long postId) {
		postService.delete(postId);
	}
}

package com.bootsignal.domain.mypage.controller;

import com.bootsignal.domain.mypage.dto.MyPageActivityResponse;
import com.bootsignal.domain.mypage.service.MyPageService;
import com.bootsignal.domain.post.entity.PostType;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 활동 조회 컨트롤러입니다.
 *
 * - GET /api/mypage/posts    : 내가 쓴 게시글 (BOARD, QNA, PROJECT_RECRUIT)
 * - GET /api/mypage/reviews  : 내가 쓴 리뷰
 * - GET /api/mypage/comments : 내가 쓴 댓글
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

	/** 게시글 type 파라미터 허용 값 — PostType enum 이름과 동일 */
	private static final Set<String> ALLOWED_POST_TYPES =
		Set.of("BOARD", "QNA", "PROJECT_RECRUIT");

	private final MyPageService myPageService;

	/**
	 * 내가 쓴 게시글 목록을 조회합니다.
	 *
	 * @param type BOARD | QNA | PROJECT_RECRUIT (생략 시 전체)
	 */
	@GetMapping("/posts")
	public MyPageActivityResponse getMyPosts(
		@RequestParam(required = false) String type,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(defaultValue = "createdAt,desc") String sort
	) {
		if (type != null && !ALLOWED_POST_TYPES.contains(type.toUpperCase())) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST,
				"유효하지 않은 type 값입니다. 허용 값: BOARD, QNA, PROJECT_RECRUIT");
		}
		if (size < 1 || size > 100) {
			throw new BootSignalException(ErrorCode.INVALID_PAGE_PARAMETER);
		}

		PostType postType = (type != null) ? PostType.valueOf(type.toUpperCase()) : null;
		return myPageService.getMyPosts(postType, buildPageable(page, size, sort));
	}

	/**
	 * 내가 쓴 리뷰 목록을 조회합니다.
	 */
	@GetMapping("/reviews")
	public MyPageActivityResponse getMyReviews(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(defaultValue = "createdAt,desc") String sort
	) {
		if (size < 1 || size > 100) {
			throw new BootSignalException(ErrorCode.INVALID_PAGE_PARAMETER);
		}
		return myPageService.getMyReviews(buildPageable(page, size, sort));
	}

	/**
	 * 내가 쓴 댓글 목록을 조회합니다.
	 */
	@GetMapping("/comments")
	public MyPageActivityResponse getMyComments(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(defaultValue = "createdAt,desc") String sort
	) {
		if (size < 1 || size > 100) {
			throw new BootSignalException(ErrorCode.INVALID_PAGE_PARAMETER);
		}
		return myPageService.getMyComments(buildPageable(page, size, sort));
	}

	/** sort 파라미터를 Pageable 로 변환합니다. 파싱 실패 시 기본 정렬(createdAt DESC)을 사용합니다. */
	private Pageable buildPageable(int page, int size, String sort) {
		try {
			String[] parts = sort.split(",");
			String property = parts[0].trim();
			Sort.Direction direction = parts.length > 1
				? Sort.Direction.fromString(parts[1].trim())
				: Sort.Direction.DESC;
			return PageRequest.of(page, size, Sort.by(direction, property));
		} catch (Exception e) {
			return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		}
	}
}

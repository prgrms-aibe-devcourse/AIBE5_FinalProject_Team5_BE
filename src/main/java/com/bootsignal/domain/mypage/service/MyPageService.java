package com.bootsignal.domain.mypage.service;

import com.bootsignal.domain.comment.entity.Comment;
import com.bootsignal.domain.comment.repository.CommentRepository;
import com.bootsignal.domain.mypage.dto.MyPageActivityResponse;
import com.bootsignal.domain.mypage.dto.MyPageItemResponse;
import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.entity.PostType;
import com.bootsignal.domain.post.repository.PostRepository;
import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.repository.ReviewRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지 활동 조회 서비스입니다.
 * 현재 로그인 사용자의 게시글, 리뷰, 댓글을 각각 페이지 단위로 조회합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final ReviewRepository reviewRepository;

	// ── 게시글 조회 ────────────────────────────────────────────

	/**
	 * 현재 사용자가 작성한 게시글 목록을 조회합니다.
	 *
	 * @param postType 게시글 유형 필터 (BOARD, QNA, PROJECT_RECRUIT). null 이면 전체 반환.
	 * @param pageable 페이지·정렬 정보
	 */
	public MyPageActivityResponse getMyPosts(PostType postType, Pageable pageable) {
		User user = getAuthenticatedUser();

		Page<Post> postPage = postRepository.findAllByUser(user.getId(), postType, pageable);

		List<MyPageItemResponse> items = postPage.getContent().stream()
			.map(p -> new MyPageItemResponse(
				p.getId(),
				p.getPostType().name(),
				p.getTitle(),
				p.getCreatedAt(),
				p.getUpdatedAt(),
				isOwnerOrAdmin(p.getUser().getId(), user)
			))
			.toList();

		return toResponse(items, postPage);
	}

	// ── 리뷰 조회 ─────────────────────────────────────────────

	/**
	 * 현재 사용자가 작성한 리뷰 목록을 조회합니다.
	 *
	 * @param pageable 페이지·정렬 정보
	 */
	public MyPageActivityResponse getMyReviews(Pageable pageable) {
		User user = getAuthenticatedUser();

		Page<Review> reviewPage = reviewRepository.findAllByUser(user.getId(), pageable);

		List<MyPageItemResponse> items = reviewPage.getContent().stream()
			.map(r -> new MyPageItemResponse(
				r.getId(),
				"REVIEW",
				summarize(r.getContent()),
				r.getCreatedAt(),
				r.getUpdatedAt(),
				isOwnerOrAdmin(r.getUser().getId(), user)
			))
			.toList();

		return toResponse(items, reviewPage);
	}

	// ── 댓글 조회 ─────────────────────────────────────────────

	/**
	 * 현재 사용자가 작성한 댓글 목록을 조회합니다.
	 *
	 * @param pageable 페이지·정렬 정보
	 */
	public MyPageActivityResponse getMyComments(Pageable pageable) {
		User user = getAuthenticatedUser();

		Page<Comment> commentPage = commentRepository.findAllByUser(user.getId(), pageable);

		List<MyPageItemResponse> items = commentPage.getContent().stream()
			.map(c -> new MyPageItemResponse(
				c.getId(),
				"COMMENT",
				summarize(c.getContent()),
				c.getCreatedAt(),
				c.getUpdatedAt(),
				isOwnerOrAdmin(c.getUser().getId(), user)
			))
			.toList();

		return toResponse(items, commentPage);
	}

	// ── 내부 헬퍼 ─────────────────────────────────────────────

	/** SecurityContext 에서 현재 사용자를 조회합니다. */
	private User getAuthenticatedUser() {
		String email = SecurityUtil.getCurrentUserEmail();
		return userRepository.findByEmail(email)
			.filter(u -> !u.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
	}

	/** 작성자 본인이거나 ADMIN 이면 삭제 가능 */
	private boolean isOwnerOrAdmin(Long ownerId, User currentUser) {
		return ownerId.equals(currentUser.getId())
			|| currentUser.getRole() == UserRole.ADMIN;
	}

	/** 내용을 최대 100자로 요약합니다. */
	private String summarize(String content) {
		if (content == null) return "";
		return content.length() > 100 ? content.substring(0, 100) + "…" : content;
	}

	/** Page 객체를 MyPageActivityResponse 로 변환합니다. */
	private <T> MyPageActivityResponse toResponse(List<MyPageItemResponse> items, Page<T> page) {
		return new MyPageActivityResponse(
			items,
			page.getTotalElements(),
			page.getTotalPages(),
			page.getNumber()
		);
	}
}

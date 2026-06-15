package com.bootsignal.domain.comment.service;

import com.bootsignal.domain.comment.dto.CommentCreateRequest;
import com.bootsignal.domain.comment.dto.CommentResponse;
import com.bootsignal.domain.comment.dto.CommentUpdateRequest;
import com.bootsignal.domain.comment.entity.Comment;
import com.bootsignal.domain.comment.repository.CommentRepository;
import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.repository.PostRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 작성, 조회, 수정, 삭제와 작성자/관리자 권한 검증을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@Transactional
	public CommentResponse create(Long postId, CommentCreateRequest request) {
		User user = getAuthenticatedUser();
		Post post = findActivePost(postId);

		Comment comment = Comment.builder()
			.post(post)
			.user(user)
			.content(request.content())
			.build();

		return CommentResponse.from(commentRepository.save(comment));
	}

	public Page<CommentResponse> getList(Long postId, Pageable pageable) {
		findActivePost(postId);
		return commentRepository.findAllActiveByPostId(postId, pageable)
			.map(CommentResponse::from);
	}

	@Transactional
	public CommentResponse update(Long commentId, CommentUpdateRequest request) {
		User user = getAuthenticatedUser();
		Comment comment = findActiveComment(commentId);
		validateEditable(user, comment);

		comment.update(request.content());
		return CommentResponse.from(comment);
	}

	@Transactional
	public void delete(Long commentId) {
		User user = getAuthenticatedUser();
		Comment comment = findActiveComment(commentId);
		validateEditable(user, comment);

		comment.softDelete();
	}

	private User getAuthenticatedUser() {
		String email = SecurityUtil.getCurrentUserEmail();
		return userRepository.findByEmail(email)
			.filter(user -> !user.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
	}

	private Post findActivePost(Long postId) {
		return postRepository.findById(postId)
			.filter(post -> post.getDeletedAt() == null && post.isValid())
			.orElseThrow(() -> new BootSignalException(ErrorCode.POST_NOT_FOUND));
	}

	private Comment findActiveComment(Long commentId) {
		return commentRepository.findActiveById(commentId)
			.orElseThrow(() -> new BootSignalException(ErrorCode.COMMENT_NOT_FOUND));
	}

	private void validateEditable(User user, Comment comment) {
		if (!comment.getUser().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
			throw new BootSignalException(ErrorCode.FORBIDDEN);
		}
	}
}

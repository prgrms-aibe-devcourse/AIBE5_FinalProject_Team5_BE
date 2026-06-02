package com.bootsignal.domain.post.service;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.post.dto.PostCreateRequest;
import com.bootsignal.domain.post.dto.PostResponse;
import com.bootsignal.domain.post.dto.PostUpdateRequest;
import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.entity.PostType;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final CourseRepository courseRepository;

	@Transactional
	public PostResponse create(PostCreateRequest request) {
		User user = getAuthenticatedUser();

		Course course = null;
		if (request.courseId() != null) {
			course = courseRepository.findById(request.courseId())
				.orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND, "해당 코스를 찾을 수 없습니다."));
		}

		Post post = Post.builder()
			.user(user)
			.course(course)
			.postType(request.postType())
			.category(request.category())
			.title(request.title())
			.content(request.content())
			.build();

		return PostResponse.from(postRepository.save(post));
	}

	public Page<PostResponse> getList(PostType postType, Long courseId, String keyword, Pageable pageable) {
		return postRepository.findAllActive(postType, courseId, keyword, pageable)
			.map(PostResponse::from);
	}

	public PostResponse get(Long postId) {
		return PostResponse.from(findActivePost(postId));
	}

	@Transactional
	public PostResponse update(Long postId, PostUpdateRequest request) {
		User user = getAuthenticatedUser();
		Post post = findActivePost(postId);

		if (!post.getUser().getId().equals(user.getId())) {
			throw new BootSignalException(ErrorCode.FORBIDDEN);
		}

		post.update(request.title(), request.content(), request.category());
		return PostResponse.from(post);
	}

	@Transactional
	public void delete(Long postId) {
		User user = getAuthenticatedUser();
		Post post = findActivePost(postId);

		if (!post.getUser().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
			throw new BootSignalException(ErrorCode.FORBIDDEN);
		}

		post.softDelete();
	}

	private User getAuthenticatedUser() {
		String email = SecurityUtil.getCurrentUserEmail();
		return userRepository.findByEmail(email)
			.filter(user -> !user.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.UNAUTHORIZED));
	}

	private Post findActivePost(Long postId) {
		return postRepository.findById(postId)
			.filter(p -> p.getDeletedAt() == null && p.isValid())
			.orElseThrow(() -> new BootSignalException(ErrorCode.POST_NOT_FOUND));
	}
}

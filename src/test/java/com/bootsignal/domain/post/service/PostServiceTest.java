package com.bootsignal.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	@Mock
	private PostRepository postRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CourseRepository courseRepository;

	private PostService postService;

	@BeforeEach
	void setUp() {
		postService = new PostService(postRepository, userRepository, courseRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createUsesJwtAuthenticatedUserEmail() {
		User user = localUser(1L, "user@example.com", UserRole.USER);
		setAuthentication("USER@Example.COM", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
			Post post = invocation.getArgument(0);
			ReflectionTestUtils.setField(post, "id", 10L);
			return post;
		});

		PostResponse response = postService.create(new PostCreateRequest(
			null,
			PostType.BOARD,
			"게시글 제목",
			"게시글 내용"
		));

		assertThat(response.postId()).isEqualTo(10L);
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.userNickname()).isEqualTo("tester");
		assertThat(response.title()).isEqualTo("게시글 제목");
	}

	@Test
	void createThrowsUnauthorizedWhenAuthenticatedEmailDoesNotExist() {
		setAuthentication("missing@example.com", UserRole.USER);
		given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

		assertThatThrownBy(() -> postService.create(new PostCreateRequest(
			null,
			PostType.BOARD,
			"게시글 제목",
			"게시글 내용"
		)))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	void updateThrowsForbiddenWhenAuthenticatedUserIsNotAuthor() {
		User currentUser = localUser(1L, "user@example.com", UserRole.USER);
		User author = localUser(2L, "author@example.com", UserRole.USER);
		Post post = post(author);
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(currentUser));
		given(postRepository.findById(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> postService.update(10L, new PostUpdateRequest("수정 제목", null, null)))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void deleteAllowsAdminWhenAuthenticatedUserIsNotAuthor() {
		User admin = localUser(1L, "admin@example.com", UserRole.ADMIN);
		User author = localUser(2L, "author@example.com", UserRole.USER);
		Post post = post(author);
		setAuthentication("admin@example.com", UserRole.ADMIN);
		given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
		given(postRepository.findById(10L)).willReturn(Optional.of(post));

		postService.delete(10L);

		assertThat(post.isValid()).isFalse();
		assertThat(post.getDeletedAt()).isNotNull();
	}

	private void setAuthentication(String email, UserRole role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
			email,
			"token",
			List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
		));
	}

	private User localUser(Long id, String email, UserRole role) {
		User user = User.signupLocal(email, "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "role", role);
		return user;
	}

	private Post post(User author) {
		Post post = Post.builder()
			.user(author)
			.postType(PostType.BOARD)
			.title("게시글 제목")
			.content("게시글 내용")
			.build();
		ReflectionTestUtils.setField(post, "id", 10L);
		return post;
	}
}

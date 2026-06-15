package com.bootsignal.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bootsignal.domain.comment.dto.CommentCreateRequest;
import com.bootsignal.domain.comment.dto.CommentResponse;
import com.bootsignal.domain.comment.dto.CommentUpdateRequest;
import com.bootsignal.domain.comment.entity.Comment;
import com.bootsignal.domain.comment.repository.CommentRepository;
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

/**
 * 댓글 서비스의 인증 사용자 조회, 권한 검증과 소프트 삭제 동작을 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

	@Mock
	private CommentRepository commentRepository;

	@Mock
	private PostRepository postRepository;

	@Mock
	private UserRepository userRepository;

	private CommentService commentService;

	@BeforeEach
	void setUp() {
		commentService = new CommentService(commentRepository, postRepository, userRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createUsesAuthenticatedUserAndActivePost() {
		User user = localUser(1L, "writer@example.com", "writer", UserRole.USER);
		Post post = post(10L, user);
		setAuthentication("WRITER@Example.COM", UserRole.USER);
		given(userRepository.findByEmail("writer@example.com")).willReturn(Optional.of(user));
		given(postRepository.findById(10L)).willReturn(Optional.of(post));
		given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
			Comment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 100L);
			return comment;
		});

		CommentResponse response = commentService.create(10L, new CommentCreateRequest("댓글 내용"));

		assertThat(response.commentId()).isEqualTo(100L);
		assertThat(response.postId()).isEqualTo(10L);
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.userNickname()).isEqualTo("writer");
		assertThat(response.content()).isEqualTo("댓글 내용");
	}

	@Test
	void createThrowsPostNotFoundWhenPostIsDeleted() {
		User user = localUser(1L, "writer@example.com", "writer", UserRole.USER);
		Post deletedPost = post(10L, user);
		deletedPost.softDelete();
		setAuthentication("writer@example.com", UserRole.USER);
		given(userRepository.findByEmail("writer@example.com")).willReturn(Optional.of(user));
		given(postRepository.findById(10L)).willReturn(Optional.of(deletedPost));

		assertThatThrownBy(() -> commentService.create(10L, new CommentCreateRequest("댓글 내용")))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.POST_NOT_FOUND);
	}

	@Test
	void updateThrowsForbiddenWhenAuthenticatedUserIsNotAuthor() {
		User currentUser = localUser(1L, "user@example.com", "user", UserRole.USER);
		User author = localUser(2L, "author@example.com", "author", UserRole.USER);
		Comment comment = comment(100L, post(10L, author), author, "기존 댓글");
		setAuthentication("user@example.com", UserRole.USER);
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(currentUser));
		given(commentRepository.findActiveById(100L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() -> commentService.update(100L, new CommentUpdateRequest("수정 댓글")))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void updateAllowsAdminWhenAuthenticatedUserIsNotAuthor() {
		User admin = localUser(1L, "admin@example.com", "admin", UserRole.ADMIN);
		User author = localUser(2L, "author@example.com", "author", UserRole.USER);
		Comment comment = comment(100L, post(10L, author), author, "기존 댓글");
		setAuthentication("admin@example.com", UserRole.ADMIN);
		given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(admin));
		given(commentRepository.findActiveById(100L)).willReturn(Optional.of(comment));

		CommentResponse response = commentService.update(100L, new CommentUpdateRequest("관리자 수정 댓글"));

		assertThat(response.content()).isEqualTo("관리자 수정 댓글");
		assertThat(comment.getContent()).isEqualTo("관리자 수정 댓글");
	}

	@Test
	void deleteSoftDeletesCommentWhenAuthorRequests() {
		User author = localUser(2L, "author@example.com", "author", UserRole.USER);
		Comment comment = comment(100L, post(10L, author), author, "기존 댓글");
		setAuthentication("author@example.com", UserRole.USER);
		given(userRepository.findByEmail("author@example.com")).willReturn(Optional.of(author));
		given(commentRepository.findActiveById(100L)).willReturn(Optional.of(comment));

		commentService.delete(100L);

		assertThat(comment.isValid()).isFalse();
		assertThat(comment.getDeletedAt()).isNotNull();
	}

	@Test
	void deleteThrowsCommentNotFoundWhenCommentDoesNotExist() {
		User author = localUser(2L, "author@example.com", "author", UserRole.USER);
		setAuthentication("author@example.com", UserRole.USER);
		given(userRepository.findByEmail("author@example.com")).willReturn(Optional.of(author));
		given(commentRepository.findActiveById(100L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.delete(100L))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
	}

	private void setAuthentication(String email, UserRole role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
			email,
			"token",
			List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
		));
	}

	private User localUser(Long id, String email, String nickname, UserRole role) {
		User user = User.signupLocal(email, "encoded-password", nickname);
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "role", role);
		return user;
	}

	private Post post(Long id, User author) {
		Post post = Post.builder()
			.user(author)
			.postType(PostType.BOARD)
			.category("자유")
			.title("게시글 제목")
			.content("게시글 내용")
			.build();
		ReflectionTestUtils.setField(post, "id", id);
		return post;
	}

	private Comment comment(Long id, Post post, User author, String content) {
		Comment comment = Comment.builder()
			.post(post)
			.user(author)
			.content(content)
			.build();
		ReflectionTestUtils.setField(comment, "id", id);
		return comment;
	}
}

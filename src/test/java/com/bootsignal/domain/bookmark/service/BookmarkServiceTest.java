package com.bootsignal.domain.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.bootsignal.domain.bookmark.dto.BookmarkCreateResponse;
import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.bookmark.repository.BookmarkRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkService 테스트")
class BookmarkServiceTest {

	@Mock
	private BookmarkRepository bookmarkRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CourseSessionRepository courseSessionRepository;

	private BookmarkService bookmarkService;

	@BeforeEach
	void setUp() {
		bookmarkService = new BookmarkService(
			bookmarkRepository,
			userRepository,
			courseSessionRepository
		);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("북마크 생성 성공 — bookmarkId, courseSessionId, createdAt 반환")
	void createReturnsBookmarkCreateResponse() {
		User user = localUser(1L, "user@example.com");
		CourseSession courseSession = courseSession(10L);
		setAuthentication("user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(courseSessionRepository.findById(10L)).willReturn(Optional.of(courseSession));
		given(bookmarkRepository.existsByUserIdAndCourseSessionId(1L, 10L)).willReturn(false);
		given(bookmarkRepository.save(any(Bookmark.class))).willAnswer(invocation -> {
			Bookmark bookmark = invocation.getArgument(0);
			ReflectionTestUtils.setField(bookmark, "id", 1L);
			ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.of(2026, 6, 5, 10, 30));
			return bookmark;
		});

		BookmarkCreateResponse response = bookmarkService.create(10L);

		assertThat(response.bookmarkId()).isEqualTo(1L);
		assertThat(response.courseSessionId()).isEqualTo(10L);
		assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 6, 5, 10, 30));
	}

	@Test
	@DisplayName("북마크 생성 실패 — 미인증 사용자 (401 UNAUTHORIZED)")
	void createThrowsUnauthorizedWhenUserIsNotAuthenticated() {
		assertThatThrownBy(() -> bookmarkService.create(10L))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("북마크 생성 실패 — 존재하지 않는 회차 (404 COURSE_SESSION_NOT_FOUND)")
	void createThrowsCourseSessionNotFoundWhenSessionDoesNotExist() {
		User user = localUser(1L, "user@example.com");
		setAuthentication("user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(courseSessionRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> bookmarkService.create(999L))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.COURSE_SESSION_NOT_FOUND);
	}

	@Test
	@DisplayName("북마크 생성 실패 — 이미 북마크한 회차 (409 BOOKMARK_ALREADY_EXISTS)")
	void createThrowsBookmarkAlreadyExistsWhenDuplicate() {
		User user = localUser(1L, "user@example.com");
		CourseSession courseSession = courseSession(10L);
		setAuthentication("user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(courseSessionRepository.findById(10L)).willReturn(Optional.of(courseSession));
		given(bookmarkRepository.existsByUserIdAndCourseSessionId(1L, 10L)).willReturn(true);

		assertThatThrownBy(() -> bookmarkService.create(10L))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.BOOKMARK_ALREADY_EXISTS);
	}

	private void setAuthentication(String email) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
			email,
			"token",
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		));
	}

	private User localUser(Long id, String email) {
		User user = User.signupLocal(email, "encoded-password", "tester");
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "role", UserRole.USER);
		return user;
	}

	private CourseSession courseSession(Long id) {
		CourseSession courseSession = CourseSession.builder()
			.trprId("TR001")
			.trprDegr(1)
			.traStartDate(LocalDate.of(2026, 7, 1))
			.traEndDate(LocalDate.of(2026, 12, 31))
			.build();
		ReflectionTestUtils.setField(courseSession, "id", id);
		return courseSession;
	}
}

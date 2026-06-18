package com.bootsignal.domain.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.bookmark.dto.BookmarkCreateResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkDeleteResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkListResponse;
import com.bootsignal.domain.bookmark.dto.BookmarkPageResponse;
import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.bookmark.repository.BookmarkRepository;
import com.bootsignal.domain.calendar.service.CalendarEventSyncService;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.institution.entity.Institution;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/* BookmarkService의 북마크 생성, 삭제, 목록 조회 정책을 검증하는 단위 테스트 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkService 테스트")
class BookmarkServiceTest {

	@Mock
	private BookmarkRepository bookmarkRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CourseSessionRepository courseSessionRepository;

	@Mock
	private CalendarEventSyncService calendarEventSyncService;

	private BookmarkService bookmarkService;

	@BeforeEach
	void setUp() {
		bookmarkService = new BookmarkService(
			bookmarkRepository,
			userRepository,
			courseSessionRepository,
			calendarEventSyncService
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

	@Test
	@DisplayName("북마크 삭제 성공 — courseSessionId 반환")
	void deleteReturnsBookmarkDeleteResponse() {
		User user = localUser(1L, "user@example.com");
		CourseSession courseSession = courseSession(10L);
		Bookmark bookmark = Bookmark.builder()
			.user(user)
			.courseSession(courseSession)
			.startDate(LocalDate.of(2026, 7, 1))
			.endDate(LocalDate.of(2026, 12, 31))
			.build();
		setAuthentication("user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(bookmarkRepository.findByUserIdAndCourseSessionId(1L, 10L)).willReturn(Optional.of(bookmark));

		BookmarkDeleteResponse response = bookmarkService.delete(10L);

		assertThat(response.courseSessionId()).isEqualTo(10L);
		verify(bookmarkRepository).delete(bookmark);
	}

	@Test
	@DisplayName("북마크 삭제 실패 — 미인증 사용자 (401 UNAUTHORIZED)")
	void deleteThrowsUnauthorizedWhenUserIsNotAuthenticated() {
		assertThatThrownBy(() -> bookmarkService.delete(10L))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("북마크 삭제 실패 — 북마크 없음 (404 BOOKMARK_NOT_FOUND)")
	void deleteThrowsBookmarkNotFoundWhenBookmarkDoesNotExist() {
		User user = localUser(1L, "user@example.com");
		setAuthentication("user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(bookmarkRepository.findByUserIdAndCourseSessionId(1L, 10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> bookmarkService.delete(10L))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.BOOKMARK_NOT_FOUND);
	}

	@Test
	@DisplayName("북마크 목록 조회 성공 — 페이징 및 연관 정보 반환")
	void getListReturnsBookmarkPageResponse() {
		User user = localUser(1L, "user@example.com");
		Bookmark bookmark = bookmarkWithRelations(user, 1L);
		Page<Bookmark> page = new PageImpl<>(List.of(bookmark), Pageable.ofSize(20), 1);
		setAuthentication("user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(bookmarkRepository.findAllByUserId(eq(1L), any(Pageable.class))).willReturn(page);

		BookmarkPageResponse<BookmarkListResponse> response = bookmarkService.getList(0, 20, "latest");

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().bookmarkId()).isEqualTo(1L);
		assertThat(response.content().getFirst().course().title()).isEqualTo("백엔드 개발자 양성과정");
		assertThat(response.content().getFirst().institution().institutionName()).isEqualTo("한국소프트웨어교육원");
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.last()).isTrue();

		// latest 정렬은 사용자가 곧 시작하는 과정을 먼저 보도록 시작일 오름차순을 사용한다.
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(bookmarkRepository).findAllByUserId(eq(1L), pageableCaptor.capture());
		Sort.Order startDateOrder = pageableCaptor.getValue().getSort().getOrderFor("startDate");
		assertThat(startDateOrder).isNotNull();
		assertThat(startDateOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	@DisplayName("북마크 목록 조회 실패 — 잘못된 페이지 정보 (400 INVALID_PAGE_PARAMETER)")
	void getListThrowsInvalidPageParameterWhenPageIsNegative() {
		assertThatThrownBy(() -> bookmarkService.getList(-1, 20, "latest"))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_PAGE_PARAMETER);
	}

	@Test
	@DisplayName("북마크 목록 조회 실패 — 잘못된 정렬 값 (400 INVALID_PAGE_PARAMETER)")
	void getListThrowsInvalidPageParameterWhenSortIsInvalid() {
		assertThatThrownBy(() -> bookmarkService.getList(0, 20, "invalid"))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.INVALID_PAGE_PARAMETER);
	}

	@Test
	@DisplayName("북마크 목록 조회 실패 — 미인증 사용자 (401 UNAUTHORIZED)")
	void getListThrowsUnauthorizedWhenUserIsNotAuthenticated() {
		assertThatThrownBy(() -> bookmarkService.getList(0, 20, "latest"))
			.isInstanceOf(BootSignalException.class)
			.extracting(exception -> ((BootSignalException) exception).errorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
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

	private Bookmark bookmarkWithRelations(User user, Long bookmarkId) {
		Institution institution = Institution.builder()
			.instCd("INST001")
			.institutionName("한국소프트웨어교육원")
			.address("서울특별시 강남구")
			.profileImageUrl("https://example.com/profile.png")
			.build();
		ReflectionTestUtils.setField(institution, "id", 5L);

		Course course = Course.builder()
			.trprId("TR001")
			.title("백엔드 개발자 양성과정")
			.subTitle("한국소프트웨어교육원")
			.stdgScor(BigDecimal.valueOf(4.7))
			.institution(institution)
			.build();
		ReflectionTestUtils.setField(course, "id", 10L);

		CourseSession courseSession = CourseSession.builder()
			.trprId("TR001")
			.trprDegr(3)
			.recruitmentCount(30)
			.selectedTraineeCount(24)
			.confirmedTraineeCount(20)
			.selfPaymentAmount(0)
			.traStartDate(LocalDate.of(2026, 7, 1))
			.traEndDate(LocalDate.of(2026, 12, 31))
			.course(course)
			.build();
		ReflectionTestUtils.setField(courseSession, "id", 1L);

		Bookmark bookmark = Bookmark.builder()
			.user(user)
			.courseSession(courseSession)
			.startDate(LocalDate.of(2026, 7, 1))
			.endDate(LocalDate.of(2026, 12, 31))
			.build();
		ReflectionTestUtils.setField(bookmark, "id", bookmarkId);
		ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.of(2026, 6, 5, 10, 30));
		return bookmark;
	}
}

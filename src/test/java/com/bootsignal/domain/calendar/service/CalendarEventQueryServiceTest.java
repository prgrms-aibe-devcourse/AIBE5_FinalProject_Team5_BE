package com.bootsignal.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.bookmark.repository.BookmarkRepository;
import com.bootsignal.domain.calendar.client.GoogleCalendarApiClient;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarListedEvent;
import com.bootsignal.domain.calendar.dto.CalendarEventItemResponse;
import com.bootsignal.domain.calendar.dto.CalendarEventListResponse;
import com.bootsignal.domain.calendar.entity.CalendarEventLog;
import com.bootsignal.domain.calendar.entity.CalendarEventLogStatus;
import com.bootsignal.domain.calendar.entity.CalendarEventType;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.CalendarEventLogRepository;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarEventQueryService 테스트")
class CalendarEventQueryServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private GoogleCalendarTokenRepository googleCalendarTokenRepository;

	@Mock
	private CalendarEventLogRepository calendarEventLogRepository;

	@Mock
	private GoogleCalendarAccessTokenService googleCalendarAccessTokenService;

	@Mock
	private GoogleCalendarApiClient googleCalendarApiClient;

	@Mock
	private BookmarkRepository bookmarkRepository;

	@InjectMocks
	private CalendarEventQueryService calendarEventQueryService;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("비구글 사용자 — 북마크 기반 일정 반환")
	void getMonthlyEventsReturnsBookmarkEventsForNonGoogleUser() {
		setAuthenticatedUser("user@example.com");
		User user = localUser(1L, "user@example.com");
		Bookmark bookmark = bookmark(user, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 12, 31));
		CalendarEventLog courseStartLog = createdLog(user, CalendarEventType.COURSE_START, "66erb1l4lfkbhk05th02s7idtc", 1L);

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(bookmarkRepository.findAllByUserIdAndDateRangeOverlap(
			1L,
			LocalDate.of(2026, 9, 1),
			LocalDate.of(2026, 9, 30)
		)).willReturn(List.of(bookmark));
		given(calendarEventLogRepository.findAllByUser_IdAndCourseSession_IdInAndStatus(
			1L,
			List.of(1L),
			CalendarEventLogStatus.CREATED
		)).willReturn(List.of(courseStartLog));

		CalendarEventListResponse response = calendarEventQueryService.getMonthlyEvents(2026, 9);

		assertThat(response.googleUser()).isFalse();
		assertThat(response.calendarConnected()).isFalse();
		assertThat(response.totalCount()).isEqualTo(1);
		assertThat(response.events()).hasSize(1);
		assertThat(response.events().get(0).id()).isEqualTo(100L);
		assertThat(response.events().get(0).googleEventId()).isEqualTo("66erb1l4lfkbhk05th02s7idtc");
		assertThat(response.events().get(0).eventType()).isEqualTo(CalendarEventType.COURSE_START);
		assertThat(response.events().get(0).courseSessionId()).isEqualTo(1L);
		verify(googleCalendarApiClient, never()).listEvents(any(), any(), any());
	}

	@Test
	@DisplayName("구글 사용자 미연동 — 북마크 기반 일정 반환")
	void getMonthlyEventsReturnsBookmarkEventsForDisconnectedGoogleUser() {
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		Bookmark bookmark = bookmark(user, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 30));
		CalendarEventLog courseStartLog = createdLog(user, CalendarEventType.COURSE_START, "google-event-start", 1L);
		CalendarEventLog courseEndLog = createdLog(user, CalendarEventType.COURSE_END, "google-event-end", 1L);

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.empty());
		given(bookmarkRepository.findAllByUserIdAndDateRangeOverlap(
			1L,
			LocalDate.of(2026, 9, 1),
			LocalDate.of(2026, 9, 30)
		)).willReturn(List.of(bookmark));
		given(calendarEventLogRepository.findAllByUser_IdAndCourseSession_IdInAndStatus(
			1L,
			List.of(1L),
			CalendarEventLogStatus.CREATED
		)).willReturn(List.of(courseStartLog, courseEndLog));

		CalendarEventListResponse response = calendarEventQueryService.getMonthlyEvents(2026, 9);

		assertThat(response.googleUser()).isTrue();
		assertThat(response.calendarConnected()).isFalse();
		assertThat(response.totalCount()).isEqualTo(2);
		assertThat(response.events()).extracting(CalendarEventItemResponse::eventType)
			.containsExactly(CalendarEventType.COURSE_START, CalendarEventType.COURSE_END);
		assertThat(response.events()).extracting(CalendarEventItemResponse::googleEventId)
			.containsExactly("google-event-start", "google-event-end");
		verify(googleCalendarApiClient, never()).listEvents(any(), any(), any());
	}

	@Test
	@DisplayName("미연동 사용자 — 북마크 없으면 빈 목록")
	void getMonthlyEventsReturnsEmptyWhenNoBookmarks() {
		setAuthenticatedUser("user@example.com");
		User user = localUser(1L, "user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
		given(bookmarkRepository.findAllByUserIdAndDateRangeOverlap(
			1L,
			LocalDate.of(2026, 6, 1),
			LocalDate.of(2026, 6, 30)
		)).willReturn(List.of());

		CalendarEventListResponse response = calendarEventQueryService.getMonthlyEvents(2026, 6);

		assertThat(response.calendarConnected()).isFalse();
		assertThat(response.totalCount()).isZero();
		assertThat(response.events()).isEmpty();
	}

	@Test
	@DisplayName("연동 사용자 — Google 일정과 로컬 메타데이터 병합")
	void getMonthlyEventsMergesGoogleEventsWithLocalLogs() {
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken token = activeToken();

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(token.isActive()).willReturn(true);
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");
		given(googleCalendarApiClient.listEvents(eq("access-token"), any(), any()))
			.willReturn(List.of(
				googleEvent(
					"abc123google",
					"[과정 시작] Spring Boot 실무",
					"BootSignal에서 자동 등록된 과정 시작 일정",
					"2026-06-01T00:00:00+09:00",
					"2026-06-01T23:59:59+09:00"
				),
				googleEvent(
					"xyz789google",
					"팀 미팅",
					"주간 스탠드업",
					"2026-06-10T14:00:00+09:00",
					"2026-06-10T15:00:00+09:00"
				)
			));

		CalendarEventLog courseStartLog = createdLog(user, CalendarEventType.COURSE_START, "abc123google", 34L);
		given(calendarEventLogRepository.findAllByUser_IdAndGoogleEventIdInAndStatus(
			1L,
			List.of("abc123google", "xyz789google"),
			CalendarEventLogStatus.CREATED
		)).willReturn(List.of(courseStartLog));

		CalendarEventListResponse response = calendarEventQueryService.getMonthlyEvents(2026, 6);

		assertThat(response.year()).isEqualTo(2026);
		assertThat(response.month()).isEqualTo(6);
		assertThat(response.googleUser()).isTrue();
		assertThat(response.calendarConnected()).isTrue();
		assertThat(response.totalCount()).isEqualTo(2);
		assertThat(response.events()).hasSize(2);

		assertThat(response.events().get(0).id()).isEqualTo(100L);
		assertThat(response.events().get(0).googleEventId()).isEqualTo("abc123google");
		assertThat(response.events().get(0).eventType()).isEqualTo(CalendarEventType.COURSE_START);
		assertThat(response.events().get(0).courseSessionId()).isEqualTo(34L);

		assertThat(response.events().get(1).id()).isNull();
		assertThat(response.events().get(1).googleEventId()).isEqualTo("xyz789google");
		assertThat(response.events().get(1).eventType()).isEqualTo(CalendarEventType.CUSTOM);
		assertThat(response.events().get(1).courseSessionId()).isNull();
	}

	@Test
	@DisplayName("month 범위 위반 — VALIDATION_ERROR")
	void getMonthlyEventsThrowsWhenMonthIsInvalid() {
		setAuthenticatedUser("user@gmail.com");

		assertThatThrownBy(() -> calendarEventQueryService.getMonthlyEvents(2026, 13))
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	@DisplayName("Google API 실패 — CALENDAR_FETCH_FAILED")
	void getMonthlyEventsThrowsWhenGoogleApiFails() {
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken token = activeToken();

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(token.isActive()).willReturn(true);
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");
		given(googleCalendarApiClient.listEvents(eq("access-token"), any(), any()))
			.willThrow(new BootSignalException(ErrorCode.CALENDAR_FETCH_FAILED));

		assertThatThrownBy(() -> calendarEventQueryService.getMonthlyEvents(2026, 6))
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CALENDAR_FETCH_FAILED);
	}

	private void setAuthenticatedUser(String email) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(
				email,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))
			)
		);
	}

	private User googleUser(Long id, String email) {
		User user = User.signupGoogle(email, "google-sub", "테스트", "테스트", null);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private User localUser(Long id, String email) {
		User user = User.signupLocal(email, "encoded-password", "테스트");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private GoogleCalendarToken activeToken() {
		return org.mockito.Mockito.mock(GoogleCalendarToken.class);
	}

	private GoogleCalendarListedEvent googleEvent(
		String id,
		String summary,
		String description,
		String startDateTime,
		String endDateTime
	) {
		return new GoogleCalendarListedEvent(
			id,
			"confirmed",
			summary,
			description,
			new GoogleCalendarListedEvent.EventDateTime(startDateTime, null, "Asia/Seoul"),
			new GoogleCalendarListedEvent.EventDateTime(endDateTime, null, "Asia/Seoul")
		);
	}

	private CalendarEventLog createdLog(
		User user,
		CalendarEventType eventType,
		String googleEventId,
		long courseSessionId
	) {
		Course course = Course.builder()
			.trprId("TR001")
			.title("Spring Boot 실무")
			.subTitle("한국소프트웨어교육원")
			.build();
		ReflectionTestUtils.setField(course, "id", 12L);

		CourseSession courseSession = CourseSession.builder()
			.trprId("TR001")
			.trprDegr(1)
			.course(course)
			.build();
		ReflectionTestUtils.setField(courseSession, "id", courseSessionId);

		LocalDateTime startAt = eventType == CalendarEventType.COURSE_END
			? LocalDateTime.of(2026, 9, 30, 0, 0)
			: courseSessionId == 34L
				? LocalDateTime.of(2026, 6, 1, 0, 0)
				: LocalDateTime.of(2026, 9, 11, 0, 0);
		LocalDateTime endAt = eventType == CalendarEventType.COURSE_END
			? LocalDateTime.of(2026, 9, 30, 23, 59, 59)
			: courseSessionId == 34L
				? LocalDateTime.of(2026, 6, 1, 23, 59, 59)
				: LocalDateTime.of(2026, 9, 11, 23, 59, 59);
		String title = eventType == CalendarEventType.COURSE_END
			? "[과정 종료] Spring Boot 실무"
			: "[과정 시작] Spring Boot 실무";

		CalendarEventLog log = CalendarEventLog.initCourseEvent(
			user,
			course,
			courseSession,
			eventType,
			title,
			startAt,
			endAt
		);
		log.syncCreated(googleEventId, title, startAt, endAt);
		ReflectionTestUtils.setField(log, "id", 100L);
		return log;
	}

	private Bookmark bookmark(User user, LocalDate startDate, LocalDate endDate) {
		Course course = Course.builder()
			.trprId("TR001")
			.title("챗GPT 생성형 AI를 활용한 반응형 웹콘텐츠(영상제작&코딩) 개발기획자 양성")
			.subTitle("한국소프트웨어교육원")
			.build();
		ReflectionTestUtils.setField(course, "id", 12L);

		CourseSession courseSession = CourseSession.builder()
			.trprId("TR001")
			.trprDegr(1)
			.course(course)
			.build();
		ReflectionTestUtils.setField(courseSession, "id", 1L);

		return Bookmark.builder()
			.user(user)
			.courseSession(courseSession)
			.startDate(startDate)
			.endDate(endDate)
			.build();
	}
}

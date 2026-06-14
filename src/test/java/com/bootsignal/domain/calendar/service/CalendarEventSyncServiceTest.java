package com.bootsignal.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.calendar.client.GoogleCalendarApiClient;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventResponse;
import com.bootsignal.domain.calendar.entity.CalendarEventLog;
import com.bootsignal.domain.calendar.entity.CalendarEventLogStatus;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.CalendarEventLogRepository;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarEventSyncService 테스트")
class CalendarEventSyncServiceTest {

	@Mock
	private GoogleCalendarTokenRepository googleCalendarTokenRepository;

	@Mock
	private CalendarEventLogRepository calendarEventLogRepository;

	@Mock
	private GoogleCalendarAccessTokenService googleCalendarAccessTokenService;

	@Mock
	private GoogleCalendarApiClient googleCalendarApiClient;

	@InjectMocks
	private CalendarEventSyncService calendarEventSyncService;

	@Test
	@DisplayName("북마크 생성 동기화 — 최초 생성 시 CREATED 로그 row 저장")
	void syncBookmarkCreatedSavesCreatedLogForGoogleCalendarUser() {
		User user = googleUser(1L);
		Bookmark bookmark = bookmark(user);
		GoogleCalendarToken token = activeToken(user);

		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(calendarEventLogRepository.findByUser_IdAndCourseSession_Id(1L, 1L)).willReturn(Optional.empty());
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");
		given(googleCalendarApiClient.createEvent(any(), any(), any(), any()))
			.willReturn(new GoogleCalendarEventResponse("google-event-1"));

		calendarEventSyncService.syncBookmarkCreated(user, bookmark);

		ArgumentCaptor<CalendarEventLog> captor = ArgumentCaptor.forClass(CalendarEventLog.class);
		verify(calendarEventLogRepository).save(captor.capture());
		CalendarEventLog savedLog = captor.getValue();

		assertThat(savedLog.getStatus()).isEqualTo(CalendarEventLogStatus.CREATED);
		assertThat(savedLog.getGoogleEventId()).isEqualTo("google-event-1");
	}

	@Test
	@DisplayName("북마크 생성 동기화 — 기존 row가 있으면 UPDATE 방식으로 CREATED 상태 갱신")
	void syncBookmarkCreatedUpdatesExistingLogRow() {
		User user = googleUser(1L);
		Bookmark bookmark = bookmark(user);
		CalendarEventLog existingLog = deletedLog(user);
		GoogleCalendarToken token = activeToken(user);

		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(calendarEventLogRepository.findByUser_IdAndCourseSession_Id(1L, 1L))
			.willReturn(Optional.of(existingLog));
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");
		given(googleCalendarApiClient.createEvent(any(), any(), any(), any()))
			.willReturn(new GoogleCalendarEventResponse("google-event-2"));

		calendarEventSyncService.syncBookmarkCreated(user, bookmark);

		assertThat(existingLog.getStatus()).isEqualTo(CalendarEventLogStatus.CREATED);
		assertThat(existingLog.getGoogleEventId()).isEqualTo("google-event-2");
		verify(calendarEventLogRepository).save(existingLog);
	}

	@Test
	@DisplayName("북마크 생성 동기화 — 비구글 사용자는 캘린더 동기화를 수행하지 않음")
	void syncBookmarkCreatedSkipsNonGoogleUser() {
		User user = localUser(1L);

		calendarEventSyncService.syncBookmarkCreated(user, bookmark(user));

		verify(calendarEventLogRepository, never()).save(any());
		verify(googleCalendarApiClient, never()).createEvent(any(), any(), any(), any());
	}

	@Test
	@DisplayName("북마크 삭제 동기화 — user + courseSession row를 DELETED 상태로 UPDATE")
	void syncBookmarkDeletedUpdatesLogRowToDeleted() {
		User user = googleUser(1L);
		CalendarEventLog eventLog = createdLog(user);
		GoogleCalendarToken token = activeToken(user);

		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(calendarEventLogRepository.findByUser_IdAndCourseSession_Id(1L, 1L))
			.willReturn(Optional.of(eventLog));
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");

		calendarEventSyncService.syncBookmarkDeleted(user, 1L);

		verify(googleCalendarApiClient).deleteEvent("access-token", "google-event-1");
		assertThat(eventLog.getStatus()).isEqualTo(CalendarEventLogStatus.DELETED);
		assertThat(eventLog.getGoogleEventId()).isNull();
	}

	private User googleUser(Long id) {
		User user = User.signupGoogle("google@example.com", "google-sub", "Google User", "google-user", null);
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "role", UserRole.USER);
		return user;
	}

	private User localUser(Long id) {
		User user = User.signupLocal("local@example.com", "encoded-password", "local-user");
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "role", UserRole.USER);
		return user;
	}

	private GoogleCalendarToken activeToken(User user) {
		GoogleCalendarToken token = GoogleCalendarToken.connect(
			user,
			"encrypted-access",
			"encrypted-refresh",
			"calendar.events",
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now()
		);
		ReflectionTestUtils.setField(token, "id", 10L);
		return token;
	}

	private Bookmark bookmark(User user) {
		Course course = Course.builder()
			.trprId("TR001")
			.title("백엔드 개발자 양성과정")
			.subTitle("한국소프트웨어교육원")
			.build();
		ReflectionTestUtils.setField(course, "id", 10L);

		CourseSession courseSession = CourseSession.builder()
			.trprId("TR001")
			.trprDegr(1)
			.traStartDate(LocalDate.of(2026, 7, 1))
			.traEndDate(LocalDate.of(2026, 12, 31))
			.course(course)
			.build();
		ReflectionTestUtils.setField(courseSession, "id", 1L);

		return Bookmark.builder()
			.user(user)
			.courseSession(courseSession)
			.startDate(LocalDate.of(2026, 7, 1))
			.endDate(LocalDate.of(2026, 12, 31))
			.build();
	}

	private CalendarEventLog createdLog(User user) {
		CalendarEventLog log = baseLog(user);
		log.syncCreated(
			"google-event-1",
			"백엔드 개발자 양성과정",
			LocalDateTime.of(2026, 7, 1, 0, 0),
			LocalDateTime.of(2026, 12, 31, 23, 59, 59)
		);
		ReflectionTestUtils.setField(log, "id", 100L);
		return log;
	}

	private CalendarEventLog deletedLog(User user) {
		CalendarEventLog log = createdLog(user);
		log.markDeleted();
		return log;
	}

	private CalendarEventLog baseLog(User user) {
		Course course = Course.builder()
			.trprId("TR001")
			.title("백엔드 개발자 양성과정")
			.subTitle("한국소프트웨어교육원")
			.build();
		ReflectionTestUtils.setField(course, "id", 10L);

		CourseSession courseSession = CourseSession.builder()
			.trprId("TR001")
			.trprDegr(1)
			.course(course)
			.build();
		ReflectionTestUtils.setField(courseSession, "id", 1L);

		return CalendarEventLog.init(
			user,
			course,
			courseSession,
			"백엔드 개발자 양성과정",
			LocalDateTime.of(2026, 7, 1, 0, 0),
			LocalDateTime.of(2026, 12, 31, 23, 59, 59)
		);
	}
}

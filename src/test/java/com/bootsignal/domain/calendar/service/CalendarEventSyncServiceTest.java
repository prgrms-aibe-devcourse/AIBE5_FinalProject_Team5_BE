package com.bootsignal.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.calendar.client.GoogleCalendarApiClient;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventResponse;
import com.bootsignal.domain.calendar.entity.CalendarEventLog;
import com.bootsignal.domain.calendar.entity.CalendarEventLogStatus;
import com.bootsignal.domain.calendar.entity.CalendarEventType;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.CalendarEventLogRepository;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.entity.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
	@DisplayName("북마크 생성 동기화 — 개강·종강 이벤트 2건 생성 및 CREATED 로그 2건 저장")
	void syncBookmarkCreatedSavesCreatedLogsForCourseStartAndEnd() {
		User user = googleUser(1L);
		Bookmark bookmark = bookmark(user);
		GoogleCalendarToken token = activeToken(user);

		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(calendarEventLogRepository.findByUser_IdAndCourseSession_IdAndEventType(
			1L, 1L, CalendarEventType.COURSE_START)).willReturn(Optional.empty());
		given(calendarEventLogRepository.findByUser_IdAndCourseSession_IdAndEventType(
			1L, 1L, CalendarEventType.COURSE_END)).willReturn(Optional.empty());
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");
		given(googleCalendarApiClient.createEvent(any(), any(), any(), any()))
			.willReturn(
				new GoogleCalendarEventResponse("google-event-start"),
				new GoogleCalendarEventResponse("google-event-end")
			);

		calendarEventSyncService.syncBookmarkCreated(user, bookmark);

		ArgumentCaptor<CalendarEventLog> captor = ArgumentCaptor.forClass(CalendarEventLog.class);
		verify(calendarEventLogRepository, times(2)).save(captor.capture());
		List<CalendarEventLog> savedLogs = captor.getAllValues();

		assertThat(savedLogs).hasSize(2);
		assertThat(savedLogs).extracting(CalendarEventLog::getEventType)
			.containsExactlyInAnyOrder(CalendarEventType.COURSE_START, CalendarEventType.COURSE_END);
		assertThat(savedLogs).allMatch(log -> log.getStatus() == CalendarEventLogStatus.CREATED);

		CalendarEventLog startLog = savedLogs.stream()
			.filter(log -> log.getEventType() == CalendarEventType.COURSE_START)
			.findFirst()
			.orElseThrow();
		assertThat(startLog.getGoogleEventId()).isEqualTo("google-event-start");
		assertThat(startLog.getEventTitle()).isEqualTo("[과정 시작] 백엔드 개발자 양성과정");
		assertThat(startLog.getEventStartAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
		assertThat(startLog.getEventEndAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 23, 59, 59));

		CalendarEventLog endLog = savedLogs.stream()
			.filter(log -> log.getEventType() == CalendarEventType.COURSE_END)
			.findFirst()
			.orElseThrow();
		assertThat(endLog.getGoogleEventId()).isEqualTo("google-event-end");
		assertThat(endLog.getEventTitle()).isEqualTo("[과정 종료] 백엔드 개발자 양성과정");
		assertThat(endLog.getEventStartAt()).isEqualTo(LocalDateTime.of(2026, 12, 31, 0, 0));
		assertThat(endLog.getEventEndAt()).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59, 59));

		verify(googleCalendarApiClient).createEvent(
			eq("access-token"),
			eq("[과정 시작] 백엔드 개발자 양성과정"),
			eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
			eq(LocalDateTime.of(2026, 7, 1, 23, 59, 59))
		);
		verify(googleCalendarApiClient).createEvent(
			eq("access-token"),
			eq("[과정 종료] 백엔드 개발자 양성과정"),
			eq(LocalDateTime.of(2026, 12, 31, 0, 0)),
			eq(LocalDateTime.of(2026, 12, 31, 23, 59, 59))
		);
	}

	@Test
	@DisplayName("북마크 생성 동기화 — 기존 row가 있으면 event_type별 UPDATE 방식으로 CREATED 상태 갱신")
	void syncBookmarkCreatedUpdatesExistingLogRowsByEventType() {
		User user = googleUser(1L);
		Bookmark bookmark = bookmark(user);
		CalendarEventLog existingStartLog = deletedLog(user, CalendarEventType.COURSE_START);
		CalendarEventLog existingEndLog = deletedLog(user, CalendarEventType.COURSE_END);
		GoogleCalendarToken token = activeToken(user);

		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(calendarEventLogRepository.findByUser_IdAndCourseSession_IdAndEventType(
			1L, 1L, CalendarEventType.COURSE_START)).willReturn(Optional.of(existingStartLog));
		given(calendarEventLogRepository.findByUser_IdAndCourseSession_IdAndEventType(
			1L, 1L, CalendarEventType.COURSE_END)).willReturn(Optional.of(existingEndLog));
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");
		given(googleCalendarApiClient.createEvent(any(), any(), any(), any()))
			.willReturn(
				new GoogleCalendarEventResponse("google-event-start-2"),
				new GoogleCalendarEventResponse("google-event-end-2")
			);

		calendarEventSyncService.syncBookmarkCreated(user, bookmark);

		assertThat(existingStartLog.getStatus()).isEqualTo(CalendarEventLogStatus.CREATED);
		assertThat(existingStartLog.getGoogleEventId()).isEqualTo("google-event-start-2");
		assertThat(existingEndLog.getStatus()).isEqualTo(CalendarEventLogStatus.CREATED);
		assertThat(existingEndLog.getGoogleEventId()).isEqualTo("google-event-end-2");
		verify(calendarEventLogRepository).save(existingStartLog);
		verify(calendarEventLogRepository).save(existingEndLog);
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
	@DisplayName("북마크 삭제 동기화 — 개강·종강 CREATED 로그를 DELETED 상태로 UPDATE")
	void syncBookmarkDeletedUpdatesLogRowsToDeleted() {
		User user = googleUser(1L);
		CalendarEventLog startLog = createdLog(user, CalendarEventType.COURSE_START, "google-event-start");
		CalendarEventLog endLog = createdLog(user, CalendarEventType.COURSE_END, "google-event-end");
		GoogleCalendarToken token = activeToken(user);

		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));
		given(calendarEventLogRepository.findAllByUser_IdAndCourseSession_Id(1L, 1L))
			.willReturn(List.of(startLog, endLog));
		given(googleCalendarAccessTokenService.resolveAccessToken(1L)).willReturn("access-token");

		calendarEventSyncService.syncBookmarkDeleted(user, 1L);

		verify(googleCalendarApiClient).deleteEvent("access-token", "google-event-start");
		verify(googleCalendarApiClient).deleteEvent("access-token", "google-event-end");
		assertThat(startLog.getStatus()).isEqualTo(CalendarEventLogStatus.DELETED);
		assertThat(startLog.getGoogleEventId()).isNull();
		assertThat(endLog.getStatus()).isEqualTo(CalendarEventLogStatus.DELETED);
		assertThat(endLog.getGoogleEventId()).isNull();
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

	private CalendarEventLog createdLog(User user, CalendarEventType eventType, String googleEventId) {
		CalendarEventLog log = baseLog(user, eventType);
		if (eventType == CalendarEventType.COURSE_START) {
			log.syncCreated(
				googleEventId,
				"[과정 시작] 백엔드 개발자 양성과정",
				LocalDateTime.of(2026, 7, 1, 0, 0),
				LocalDateTime.of(2026, 7, 1, 23, 59, 59)
			);
		} else {
			log.syncCreated(
				googleEventId,
				"[과정 종료] 백엔드 개발자 양성과정",
				LocalDateTime.of(2026, 12, 31, 0, 0),
				LocalDateTime.of(2026, 12, 31, 23, 59, 59)
			);
		}
		ReflectionTestUtils.setField(log, "id", eventType == CalendarEventType.COURSE_START ? 100L : 101L);
		return log;
	}

	private CalendarEventLog deletedLog(User user, CalendarEventType eventType) {
		CalendarEventLog log = createdLog(
			user,
			eventType,
			eventType == CalendarEventType.COURSE_START ? "google-event-start" : "google-event-end"
		);
		log.markDeleted();
		return log;
	}

	private CalendarEventLog baseLog(User user, CalendarEventType eventType) {
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

		if (eventType == CalendarEventType.COURSE_START) {
			return CalendarEventLog.initCourseEvent(
				user,
				course,
				courseSession,
				eventType,
				"[과정 시작] 백엔드 개발자 양성과정",
				LocalDateTime.of(2026, 7, 1, 0, 0),
				LocalDateTime.of(2026, 7, 1, 23, 59, 59)
			);
		}

		return CalendarEventLog.initCourseEvent(
			user,
			course,
			courseSession,
			eventType,
			"[과정 종료] 백엔드 개발자 양성과정",
			LocalDateTime.of(2026, 12, 31, 0, 0),
			LocalDateTime.of(2026, 12, 31, 23, 59, 59)
		);
	}
}

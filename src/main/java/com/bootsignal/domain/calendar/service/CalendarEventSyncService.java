package com.bootsignal.domain.calendar.service;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import com.bootsignal.domain.calendar.client.GoogleCalendarApiClient;
import com.bootsignal.domain.calendar.client.dto.GoogleCalendarEventResponse;
import com.bootsignal.domain.calendar.entity.CalendarEventLog;
import com.bootsignal.domain.calendar.entity.CalendarEventType;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.CalendarEventLogRepository;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 북마크 & 구글 캘린더 동기화 
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarEventSyncService {

	private static final String COURSE_START_TITLE_PREFIX = "[과정 시작] ";
	private static final String COURSE_END_TITLE_PREFIX = "[과정 종료] ";

	private final GoogleCalendarTokenRepository googleCalendarTokenRepository;
	private final CalendarEventLogRepository calendarEventLogRepository;
	private final GoogleCalendarAccessTokenService googleCalendarAccessTokenService;
	private final GoogleCalendarApiClient googleCalendarApiClient;



	/* 북마크 생성 후 Google Calendar 동기화 */
	@Transactional
	public void syncBookmarkCreated(User user, Bookmark bookmark) {
		// 대상 여부 확인
		if (!isCalendarSyncTarget(user)) {
			return;
		}

		// 과정 정보 조회
		CourseSession courseSession = bookmark.getCourseSession();
		Course course = courseSession.getCourse();
		String courseTitle = course.getTitle();

		// 과정 시작 일정 동기화
		syncCourseEvent(
			user,
			course,
			courseSession,
			CalendarEventType.COURSE_START,
			COURSE_START_TITLE_PREFIX + courseTitle,
			bookmark.getStartDate()
		);

		// 과정 종료 일정 동기화
		syncCourseEvent(
			user,
			course,
			courseSession,
			CalendarEventType.COURSE_END,
			COURSE_END_TITLE_PREFIX + courseTitle,
			bookmark.getEndDate()
		);
	}

	/* 북마크 삭제 후 Google Calendar 동기화 */
	@Transactional
	public void syncBookmarkDeleted(User user, Long courseSessionId) {
		// 대상 여부 확인
		if (!isCalendarSyncTarget(user)) {
			return;
		}

		// 이벤트 생성 로그 조회 & 삭제 시도
		calendarEventLogRepository.findAllByUser_IdAndCourseSession_Id(user.getId(), courseSessionId)
			.stream()
			.filter(CalendarEventLog::isCreated)
			.forEach(this::deleteGoogleCalendarEvent);
	}


	// 구글 Calendar 동기화 대상 여부 확인
	public boolean isCalendarSyncTarget(User user) {
		// 구글 사용자 여부 + 캘린더 연동 상태 + 토큰 활성 여부
		return user.getProvider() == AuthProvider.GOOGLE
			&& googleCalendarTokenRepository.findByUser_Id(user.getId())
				.filter(GoogleCalendarToken::isActive)
				.isPresent();
	}

	// 과정 시작&종료 일정 동기화
	private void syncCourseEvent(
		User user,
		Course course,
		CourseSession courseSession,
		CalendarEventType eventType,
		String eventTitle,
		LocalDate eventDate
	) {
		LocalDateTime eventStartAt = eventDate.atStartOfDay();
		LocalDateTime eventEndAt = eventDate.atTime(23, 59, 59);

		try {
			String accessToken = googleCalendarAccessTokenService.resolveAccessToken(user.getId());
			GoogleCalendarEventResponse response = googleCalendarApiClient.createEvent(
				accessToken,
				eventTitle,
				eventStartAt,
				eventEndAt
			);

			if (response == null || !StringUtils.hasText(response.id())) {
				saveOrUpdateFailedLog(
					user, course, courseSession, eventType, eventTitle, eventStartAt, eventEndAt,
					"Google Calendar 이벤트 ID를 받지 못했습니다."
				);
				return;
			}

			saveOrUpdateCreatedLog(
				user, course, courseSession, eventType, response.id(), eventTitle, eventStartAt, eventEndAt
			);
		} catch (Exception exception) {
			log.warn(
				"북마크 생성 후 Google Calendar 동기화 실패. userId={}, courseSessionId={}, eventType={}",
				user.getId(), courseSession.getId(), eventType, exception
			);
			saveOrUpdateFailedLog(
				user, course, courseSession, eventType, eventTitle, eventStartAt, eventEndAt,
				resolveErrorMessage(exception)
			);
		}
	}

	// 구글 Calendar 이벤트 생성 로그 저장 & 갱신
	private void saveOrUpdateCreatedLog(
		User user,
		Course course,
		CourseSession courseSession,
		CalendarEventType eventType,
		String googleEventId,
		String eventTitle,
		LocalDateTime eventStartAt,
		LocalDateTime eventEndAt
	) {
		CalendarEventLog eventLog = findOrInitLog(
			user, course, courseSession, eventType, eventTitle, eventStartAt, eventEndAt
		);
		eventLog.syncCreated(googleEventId, eventTitle, eventStartAt, eventEndAt);
		calendarEventLogRepository.save(eventLog);
	}

 
	/* 구글 Calendar 이벤트 삭제 동기화 */
	private void deleteGoogleCalendarEvent(CalendarEventLog eventLog) {
		if (!StringUtils.hasText(eventLog.getGoogleEventId())) {
			eventLog.markDeleteFailed("삭제할 Google Calendar 이벤트 ID가 없습니다.");
			calendarEventLogRepository.save(eventLog);
			return;
		}

		try {
			String accessToken = googleCalendarAccessTokenService.resolveAccessToken(eventLog.getUser().getId());
			googleCalendarApiClient.deleteEvent(accessToken, eventLog.getGoogleEventId());
			eventLog.markDeleted();
			calendarEventLogRepository.save(eventLog);
		} catch (Exception exception) {
			log.warn(
				"북마크 삭제 후 Google Calendar 동기화 실패. logId={}, googleEventId={}, eventType={}",
				eventLog.getId(), eventLog.getGoogleEventId(), eventLog.getEventType(), exception
			);
			eventLog.markDeleteFailed(resolveErrorMessage(exception));
			calendarEventLogRepository.save(eventLog);
		}
	}

	// 구글 Calendar 이벤트 생성 실패 로그 저장
	private void saveOrUpdateFailedLog(
		User user,
		Course course,
		CourseSession courseSession,
		CalendarEventType eventType,
		String eventTitle,
		LocalDateTime eventStartAt,
		LocalDateTime eventEndAt,
		String errorMessage
	) {
		CalendarEventLog eventLog = findOrInitLog(
			user, course, courseSession, eventType, eventTitle, eventStartAt, eventEndAt
		);
		eventLog.syncFailed(eventTitle, eventStartAt, eventEndAt, errorMessage);
		calendarEventLogRepository.save(eventLog);
	}

	// 구글 Calendar 이벤트 로그 조회 또는 초기화
	private CalendarEventLog findOrInitLog(
		User user,
		Course course,
		CourseSession courseSession,
		CalendarEventType eventType,
		String eventTitle,
		LocalDateTime eventStartAt,
		LocalDateTime eventEndAt
	) {
		return calendarEventLogRepository
			.findByUser_IdAndCourseSession_IdAndEventType(user.getId(), courseSession.getId(), eventType)
			.orElseGet(() -> CalendarEventLog.initCourseEvent(
				user, course, courseSession, eventType, eventTitle, eventStartAt, eventEndAt
			));
	}

	// 예외 메시지 해석
	private String resolveErrorMessage(Exception exception) {
		String message = exception.getMessage();
		return StringUtils.hasText(message) ? message : exception.getClass().getSimpleName();
	}
}

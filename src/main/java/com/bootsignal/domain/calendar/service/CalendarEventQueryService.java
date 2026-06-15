package com.bootsignal.domain.calendar.service;

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
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// Google Calendar 이벤트 조회 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarEventQueryService {

	private static final String COURSE_START_TITLE_PREFIX = "[과정 시작] ";
	private static final String COURSE_END_TITLE_PREFIX = "[과정 종료] ";
	private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int MIN_YEAR = 1970;
	private static final int MAX_YEAR = 2100;

	private final UserRepository userRepository;
	private final GoogleCalendarTokenRepository googleCalendarTokenRepository;
	private final CalendarEventLogRepository calendarEventLogRepository;
	private final GoogleCalendarAccessTokenService googleCalendarAccessTokenService;
	private final GoogleCalendarApiClient googleCalendarApiClient;
	private final BookmarkRepository bookmarkRepository;

	/* 월별 일정 목록 조회 (Google Calendar + 로컬 메타데이터 병합) */
	public CalendarEventListResponse getMonthlyEvents(int year, int month) {
		validateYearMonth(year, month);

		User user = findActiveUser();
		boolean googleUser = user.getProvider() == AuthProvider.GOOGLE;
		boolean calendarConnected = googleUser && isCalendarConnected(user.getId());

		// 구글 캘린더 연동 상태에 따른 이벤트 목록 조회 분기 처리 
		if (!calendarConnected) {
			// 북마크 기반 이벤트 목록 조회
			return getEventsFromBookmarks(user, year, month, googleUser);
		}

		// 구글 캘린더 기반 이벤트 목록 조회
		YearMonth yearMonth = YearMonth.of(year, month);
		String timeMin = toGoogleTimeParam(yearMonth.atDay(1).atStartOfDay(ZONE_ID).toInstant());
		String timeMax = toGoogleTimeParam(yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZONE_ID).toInstant());

		String accessToken = googleCalendarAccessTokenService.resolveAccessToken(user.getId());
		List<GoogleCalendarListedEvent> googleEvents = googleCalendarApiClient.listEvents(accessToken, timeMin, timeMax);

		Map<String, CalendarEventLog> logsByGoogleEventId = findCreatedLogsByGoogleEventId(
			user.getId(),
			googleEvents
		);

		List<CalendarEventItemResponse> events = googleEvents.stream()
			.filter(this::isActiveEvent)
			.map(googleEvent -> toEventItem(googleEvent, logsByGoogleEventId.get(googleEvent.id())))
			.sorted(Comparator.comparing(CalendarEventItemResponse::startAt))
			.toList();

		return new CalendarEventListResponse(
			year,
			month,
			googleUser,
			true,
			events.size(),
			events
		);
	}

	// 북마크 기반 이벤트 목록 조회
	private CalendarEventListResponse getEventsFromBookmarks(
		User user,
		int year,
		int month,
		boolean googleUser
	) {
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate monthStart = yearMonth.atDay(1);
		LocalDate monthEnd = yearMonth.atEndOfMonth();

		List<Bookmark> bookmarks = bookmarkRepository.findAllByUserIdAndDateRangeOverlap(
			user.getId(),
			monthStart,
			monthEnd
		);

		Map<BookmarkEventLogKey, CalendarEventLog> logsByCourseSessionAndType =
			findCreatedLogsByCourseSessions(user.getId(), bookmarks);

		List<CalendarEventItemResponse> events = bookmarks.stream()
			.flatMap(bookmark -> toBookmarkEvents(bookmark, monthStart, monthEnd, logsByCourseSessionAndType).stream())
			.sorted(Comparator.comparing(CalendarEventItemResponse::startAt))
			.toList();

		return new CalendarEventListResponse(
			year,
			month,
			googleUser,
			false,
			events.size(),
			events
		);
	}

	// 북마크 기반 이벤트 목록 변환
	private List<CalendarEventItemResponse> toBookmarkEvents(
		Bookmark bookmark,
		LocalDate monthStart,
		LocalDate monthEnd,
		Map<BookmarkEventLogKey, CalendarEventLog> logsByCourseSessionAndType
	) {
		List<CalendarEventItemResponse> events = new ArrayList<>();
		CourseSession courseSession = bookmark.getCourseSession();
		String courseTitle = courseSession.getCourse().getTitle();
		Long courseSessionId = courseSession.getId();

		LocalDate startDate = bookmark.getStartDate();
		if (isWithinMonth(startDate, monthStart, monthEnd)) {
			events.add(toBookmarkEvent(
				COURSE_START_TITLE_PREFIX + courseTitle,
				startDate,
				CalendarEventType.COURSE_START,
				courseSessionId,
				logsByCourseSessionAndType.get(new BookmarkEventLogKey(courseSessionId, CalendarEventType.COURSE_START))
			));
		}

		LocalDate endDate = bookmark.getEndDate();
		if (isWithinMonth(endDate, monthStart, monthEnd)) {
			events.add(toBookmarkEvent(
				COURSE_END_TITLE_PREFIX + courseTitle,
				endDate,
				CalendarEventType.COURSE_END,
				courseSessionId,
				logsByCourseSessionAndType.get(new BookmarkEventLogKey(courseSessionId, CalendarEventType.COURSE_END))
			));
		}

		return events;
	}

	// 북마크 기반 이벤트 목록 로그 조회
	private Map<BookmarkEventLogKey, CalendarEventLog> findCreatedLogsByCourseSessions(
		Long userId,
		List<Bookmark> bookmarks
	) {
		List<Long> courseSessionIds = bookmarks.stream()
			.map(bookmark -> bookmark.getCourseSession().getId())
			.distinct()
			.toList();

		if (courseSessionIds.isEmpty()) {
			return Map.of();
		}

		return calendarEventLogRepository
			.findAllByUser_IdAndCourseSession_IdInAndStatus(userId, courseSessionIds, CalendarEventLogStatus.CREATED)
			.stream()
			.filter(log -> log.getCourseSession() != null)
			.collect(Collectors.toMap(
				log -> new BookmarkEventLogKey(log.getCourseSession().getId(), log.getEventType()),
				Function.identity()
			));
	}

	// 북마크 기반 이벤트 목록 변환
	private CalendarEventItemResponse toBookmarkEvent(
		String title,
		LocalDate eventDate,
		CalendarEventType eventType,
		Long courseSessionId,
		CalendarEventLog eventLog
	) {
		// 이벤트 로그 여부예 따른 북마크 이벤트 변환
		return new CalendarEventItemResponse(
			eventLog != null ? eventLog.getId() : null,
			eventLog != null ? eventLog.getGoogleEventId() : null,
			title,
			eventLog != null ? eventLog.getEventDescription() : null,
			eventDate.atStartOfDay(),
			eventDate.atTime(23, 59, 59),
			eventType,
			courseSessionId
		);
	}

	// 북마크 기반 이벤트 목록 로그 키
	private record BookmarkEventLogKey(Long courseSessionId, CalendarEventType eventType) {
	}

	// 날짜 범위 내 여부 확인
	private boolean isWithinMonth(LocalDate date, LocalDate monthStart, LocalDate monthEnd) {
		return !date.isBefore(monthStart) && !date.isAfter(monthEnd);
	}

	// 구글 이벤트 목록 로그 조회
	private Map<String, CalendarEventLog> findCreatedLogsByGoogleEventId(
		Long userId,
		List<GoogleCalendarListedEvent> googleEvents
	) {
		List<String> googleEventIds = googleEvents.stream()
			.map(GoogleCalendarListedEvent::id)
			.filter(StringUtils::hasText)
			.toList();

		if (googleEventIds.isEmpty()) {
			return Map.of();
		}

		return calendarEventLogRepository
			.findAllByUser_IdAndGoogleEventIdInAndStatus(userId, googleEventIds, CalendarEventLogStatus.CREATED)
			.stream()
			.filter(log -> StringUtils.hasText(log.getGoogleEventId()))
			.collect(Collectors.toMap(CalendarEventLog::getGoogleEventId, Function.identity()));
	}

	// 구글 이벤트 목록 변환
	private CalendarEventItemResponse toEventItem(GoogleCalendarListedEvent googleEvent, CalendarEventLog eventLog) {
		LocalDateTime startAt = resolveStartAt(googleEvent);
		LocalDateTime endAt = resolveEndAt(googleEvent);

		// 구글 이벤트 목록 로그 조회 결과가 있는 경우
		if (eventLog != null) { 
			return new CalendarEventItemResponse(
				eventLog.getId(),
				googleEvent.id(),
				resolveTitle(googleEvent),
				resolveDescription(googleEvent, eventLog),
				startAt,
				endAt,
				eventLog.getEventType(),
				resolveCourseSessionId(eventLog)
			);
		}

		// 구글 이벤트 목록 로그 조회 결과가 없는 경우
		return new CalendarEventItemResponse(
			null,
			googleEvent.id(),
			resolveTitle(googleEvent),
			googleEvent.description(),
			startAt,
			endAt,
			CalendarEventType.CUSTOM,
			null
		);
	}

	// 활성 이벤트 여부 확인
	private boolean isActiveEvent(GoogleCalendarListedEvent googleEvent) {
		return googleEvent != null
			&& StringUtils.hasText(googleEvent.id())
			&& !"cancelled".equalsIgnoreCase(googleEvent.status());
	}

	// 구글 이벤트 제목 해석
	private String resolveTitle(GoogleCalendarListedEvent googleEvent) {
		return StringUtils.hasText(googleEvent.summary()) ? googleEvent.summary() : "(제목 없음)";
	}

	// 구글 이벤트 설명 해석
	private String resolveDescription(GoogleCalendarListedEvent googleEvent, CalendarEventLog eventLog) {
		if (StringUtils.hasText(googleEvent.description())) {
			return googleEvent.description();
		}
		return eventLog.getEventDescription();
	}

	// 구글 이벤트 과정 회차 ID 해석
	private Long resolveCourseSessionId(CalendarEventLog eventLog) {
		CourseSession courseSession = eventLog.getCourseSession();
		return courseSession != null ? courseSession.getId() : null;
	}

	// 구글 이벤트 시작 시각 해석
	private LocalDateTime resolveStartAt(GoogleCalendarListedEvent googleEvent) {
		if (googleEvent.start() == null) {
			throw new BootSignalException(ErrorCode.CALENDAR_FETCH_FAILED, "Google Calendar 일정 시작 시각이 없습니다.");
		}

		if (StringUtils.hasText(googleEvent.start().dateTime())) {
			return OffsetDateTime.parse(googleEvent.start().dateTime())
				.atZoneSameInstant(ZONE_ID)
				.toLocalDateTime();
		}

		if (StringUtils.hasText(googleEvent.start().date())) {
			return LocalDate.parse(googleEvent.start().date()).atStartOfDay();
		}

		throw new BootSignalException(ErrorCode.CALENDAR_FETCH_FAILED, "Google Calendar 일정 시작 시각이 없습니다.");
	}

	// 구글 이벤트 종료 시각 해석
	private LocalDateTime resolveEndAt(GoogleCalendarListedEvent googleEvent) {
		if (googleEvent.end() == null) {
			throw new BootSignalException(ErrorCode.CALENDAR_FETCH_FAILED, "Google Calendar 일정 종료 시각이 없습니다.");
		}

		if (StringUtils.hasText(googleEvent.end().dateTime())) {
			return OffsetDateTime.parse(googleEvent.end().dateTime())
				.atZoneSameInstant(ZONE_ID)
				.toLocalDateTime();
		}

		if (StringUtils.hasText(googleEvent.end().date())) {
			LocalDate exclusiveEndDate = LocalDate.parse(googleEvent.end().date());
			return exclusiveEndDate.minusDays(1).atTime(23, 59, 59);
		}

		throw new BootSignalException(ErrorCode.CALENDAR_FETCH_FAILED, "Google Calendar 일정 종료 시각이 없습니다.");
	}

	// 구글 캘린더 연동 상태 확인
	private boolean isCalendarConnected(Long userId) {
		return googleCalendarTokenRepository.findByUser_Id(userId)
			.filter(GoogleCalendarToken::isActive)
			.isPresent();
	}

	// 년월 쿼리 파라미터 유효성 검증
	private void validateYearMonth(int year, int month) {
		if (year < MIN_YEAR || year > MAX_YEAR) {
			throw new BootSignalException(ErrorCode.VALIDATION_ERROR, "year는 " + MIN_YEAR + "에서 " + MAX_YEAR + " 사이여야 합니다.");
		}
		if (month < 1 || month > 12) {
			throw new BootSignalException(ErrorCode.VALIDATION_ERROR, "month는 1에서 12 사이여야 합니다.");
		}
	}

	// 현재 사용자 조회
	private User findActiveUser() {
		String email = SecurityUtil.getCurrentUserEmail();

		return userRepository.findByEmail(email)
			.filter(activeUser -> !activeUser.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.USER_NOT_FOUND));
	}

	// 구글 시간 파라미터 변환
	private String toGoogleTimeParam(Instant instant) {
		return instant.toString();
	}
}

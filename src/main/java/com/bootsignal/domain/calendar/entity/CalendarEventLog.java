package com.bootsignal.domain.calendar.entity;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "calendar_event_log",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_calendar_event_log_course_type",
		columnNames = {"user_id", "course_session_id", "event_type"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEventLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id")
	private Course course;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_session_id")
	private CourseSession courseSession;

	@Column(name = "google_event_id", length = 255)
	private String googleEventId; // 구글 Calendar 측 발급 이벤트 ID

	@Column(name = "event_title", nullable = false, length = 255)
	private String eventTitle;

	@Column(name = "event_description", columnDefinition = "TEXT")
	private String eventDescription; // 이벤트 설명

	@Column(name = "event_start_at", nullable = false)
	private LocalDateTime eventStartAt;

	@Column(name = "event_end_at", nullable = false)
	private LocalDateTime eventEndAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 30)
	private CalendarEventType eventType; // 이벤트 타입

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CalendarEventLogStatus status;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	// 구글 Calendar 이벤트 생성 로그 초기화
	public static CalendarEventLog initCourseEvent(
		User user,
		Course course,
		CourseSession courseSession,
		CalendarEventType eventType,
		String eventTitle,
		LocalDateTime eventStartAt,
		LocalDateTime eventEndAt
	) {
		CalendarEventLog log = new CalendarEventLog();
		log.user = user;
		log.course = course;
		log.courseSession = courseSession;
		log.eventType = eventType;
		log.eventTitle = eventTitle;
		log.eventStartAt = eventStartAt;
		log.eventEndAt = eventEndAt;
		log.status = CalendarEventLogStatus.FAILED;
		return log;
	}

	// 구글 Calendar 이벤트 생성 로그 동기화
	public void syncCreated(
		String googleEventId,
		String eventTitle,
		LocalDateTime eventStartAt,
		LocalDateTime eventEndAt
	) {
		this.googleEventId = googleEventId;
		this.eventTitle = eventTitle;
		this.eventStartAt = eventStartAt;
		this.eventEndAt = eventEndAt;
		this.status = CalendarEventLogStatus.CREATED;
		this.errorMessage = null;
	}

	// 구글 Calendar 이벤트 생성 로그 실패 동기화
	public void syncFailed(
		String eventTitle,
		LocalDateTime eventStartAt,
		LocalDateTime eventEndAt,
		String errorMessage
	) {
		this.googleEventId = null;
		this.eventTitle = eventTitle;
		this.eventStartAt = eventStartAt;
		this.eventEndAt = eventEndAt;
		this.status = CalendarEventLogStatus.FAILED;
		this.errorMessage = errorMessage;
	}

	// 구글 Calendar 이벤트 삭제 로그 동기화
	public void markDeleted() {
		this.status = CalendarEventLogStatus.DELETED;
		this.googleEventId = null;
		this.errorMessage = null;
	}

	// 구글 Calendar 이벤트 삭제 로그 실패 동기화
	public void markDeleteFailed(String errorMessage) {
		this.status = CalendarEventLogStatus.FAILED;
		this.errorMessage = errorMessage;
	}

	// 구글 Calendar 이벤트 생성 로그 조회
	public boolean isCreated() {
		return status == CalendarEventLogStatus.CREATED;
	}
}

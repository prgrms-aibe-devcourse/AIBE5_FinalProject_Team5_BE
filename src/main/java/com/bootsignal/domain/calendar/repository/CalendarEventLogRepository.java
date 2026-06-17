package com.bootsignal.domain.calendar.repository;

import com.bootsignal.domain.calendar.entity.CalendarEventLog;
import com.bootsignal.domain.calendar.entity.CalendarEventLogStatus;
import com.bootsignal.domain.calendar.entity.CalendarEventType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventLogRepository extends JpaRepository<CalendarEventLog, Long> {

	Optional<CalendarEventLog> findByUser_IdAndCourseSession_IdAndEventType(
		Long userId,
		Long courseSessionId,
		CalendarEventType eventType
	);

	List<CalendarEventLog> findAllByUser_IdAndCourseSession_Id(Long userId, Long courseSessionId);

	// 구글 연동 기준 이벤트 목록 조회
	List<CalendarEventLog> findAllByUser_IdAndGoogleEventIdInAndStatus(
		Long userId,
		Collection<String> googleEventIds,
		CalendarEventLogStatus status
	);

	// 구글 미연동 기준 이벤트 목록 조회
	@Query("""
		SELECT l FROM CalendarEventLog l
		WHERE l.user.id = :userId
		AND l.courseSession.id IN :courseSessionIds
		AND l.status = :status
		""")
	List<CalendarEventLog> findAllByUser_IdAndCourseSession_IdInAndStatus(
		@Param("userId") Long userId,
		@Param("courseSessionIds") Collection<Long> courseSessionIds,
		@Param("status") CalendarEventLogStatus status
	);
}

package com.bootsignal.domain.calendar.repository;

import com.bootsignal.domain.calendar.entity.CalendarEventLog;
import com.bootsignal.domain.calendar.entity.CalendarEventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventLogRepository extends JpaRepository<CalendarEventLog, Long> {

	Optional<CalendarEventLog> findByUser_IdAndCourseSession_IdAndEventType(
		Long userId,
		Long courseSessionId,
		CalendarEventType eventType
	);

	List<CalendarEventLog> findAllByUser_IdAndCourseSession_Id(Long userId, Long courseSessionId);
}

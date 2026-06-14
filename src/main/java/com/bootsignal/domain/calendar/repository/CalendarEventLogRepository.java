package com.bootsignal.domain.calendar.repository;

import com.bootsignal.domain.calendar.entity.CalendarEventLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventLogRepository extends JpaRepository<CalendarEventLog, Long> {

	Optional<CalendarEventLog> findByUser_IdAndCourseSession_Id(Long userId, Long courseSessionId);
}

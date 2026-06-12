package com.bootsignal.domain.calendar.repository;

import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, Long> {

	Optional<GoogleCalendarToken> findByUser_Id(Long userId);
}

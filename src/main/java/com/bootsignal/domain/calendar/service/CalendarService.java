package com.bootsignal.domain.calendar.service;

import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

	private final UserRepository userRepository;
	private final GoogleCalendarTokenRepository googleCalendarTokenRepository;

	/* 구글 캘린더 연동 상태 조회 */
	public CalendarStatusResponse getStatus() {
		// 현재 사용자 조회
		User user = findActiveUser();

		// 구글 사용자 여부 검사
		if (user.getProvider() != AuthProvider.GOOGLE) {
			return CalendarStatusResponse.notGoogleUser();
		}

		// 상태 조회 및 응답 반환
		return googleCalendarTokenRepository.findByUser_Id(user.getId())
			.filter(GoogleCalendarToken::isActive)
			.map(token -> CalendarStatusResponse.connected(token.getConnectedAt(), token.getExpiresAt()))
			.orElseGet(CalendarStatusResponse::notConnected);
	}


	// 현재 사용자 조회
	private User findActiveUser() {
		String email = SecurityUtil.getCurrentUserEmail();

		return userRepository.findByEmail(email)
			.filter(activeUser -> !activeUser.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.USER_NOT_FOUND));
	}
}

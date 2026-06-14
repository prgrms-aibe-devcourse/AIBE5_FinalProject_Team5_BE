package com.bootsignal.domain.calendar.service;

import com.bootsignal.domain.calendar.client.GoogleCalendarOAuthClient;
import com.bootsignal.domain.calendar.client.dto.GoogleOAuthTokenResponse;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.crypto.TokenEncryptionService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 구글 캘린더 연동 이후 토큰 관리 처리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleCalendarAccessTokenService {

	private static final long EXPIRY_BUFFER_SECONDS = 60;

	private final GoogleCalendarTokenRepository googleCalendarTokenRepository;
	private final GoogleCalendarOAuthClient googleCalendarOAuthClient;
	private final TokenEncryptionService tokenEncryptionService;

	/* 구글 캘린더 엑세스 토큰 조회 */
	@Transactional
	public String resolveAccessToken(Long userId) {
		GoogleCalendarToken token = googleCalendarTokenRepository.findByUser_Id(userId)
			.filter(GoogleCalendarToken::isActive)
			.orElseThrow(() -> new BootSignalException(ErrorCode.CALENDAR_NOT_CONNECTED));

		if (isAccessTokenValid(token.getExpiresAt())) {
			return tokenEncryptionService.decrypt(token.getAccessTokenEncrypted());
		}

		return refreshAndPersistAccessToken(token);
	}

	/* 엑세스 토큰 유효성 검증 */
	private boolean isAccessTokenValid(LocalDateTime expiresAt) {
		return expiresAt.isAfter(LocalDateTime.now().plusSeconds(EXPIRY_BUFFER_SECONDS));
	}

	/* 엑세스 토큰 갱신 및 저장 */
	private String refreshAndPersistAccessToken(GoogleCalendarToken token) {
		String refreshToken = tokenEncryptionService.decrypt(token.getRefreshTokenEncrypted());
		if (!StringUtils.hasText(refreshToken)) {
			throw new BootSignalException(ErrorCode.CALENDAR_NOT_CONNECTED);
		}

		GoogleOAuthTokenResponse tokenResponse = googleCalendarOAuthClient.refreshAccessToken(refreshToken);
		if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_EXCHANGE_FAILED);
		}

		LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.expiresIn());
		String encryptedAccessToken = tokenEncryptionService.encrypt(tokenResponse.accessToken());
		token.refreshAccessToken(encryptedAccessToken, expiresAt);

		return tokenResponse.accessToken();
	}
}

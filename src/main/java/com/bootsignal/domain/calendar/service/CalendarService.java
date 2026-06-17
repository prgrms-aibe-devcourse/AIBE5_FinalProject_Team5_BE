package com.bootsignal.domain.calendar.service;

import com.bootsignal.domain.calendar.client.GoogleCalendarOAuthClient;
import com.bootsignal.domain.calendar.client.dto.GoogleOAuthTokenResponse;
import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.config.properties.GoogleCalendarProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.SecurityUtil;
import com.bootsignal.global.security.crypto.TokenEncryptionService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// Google Calendar 연동 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

	private final UserRepository userRepository;
	private final GoogleCalendarTokenRepository googleCalendarTokenRepository;
	private final GoogleCalendarOAuthClient googleCalendarOAuthClient;
	private final GoogleCalendarOAuthStateService googleCalendarOAuthStateService;
	private final GoogleCalendarProperties googleCalendarProperties;
	private final TokenEncryptionService tokenEncryptionService;

	/* 구글 캘린더 연동 상태 조회 */
	public CalendarStatusResponse getStatus() {
		User user = findActiveUser();
		return buildStatusForUser(user);
	}

	/* Google Calendar OAuth 연동  */
	public String startGoogleConnect() {
		User user = findActiveGoogleUser();

		// 이미 연동된 경우 예외 처리
		if (googleCalendarTokenRepository.findByUser_Id(user.getId())
			.filter(GoogleCalendarToken::isActive)
			.isPresent()) {
			throw new BootSignalException(ErrorCode.CALENDAR_ALREADY_CONNECTED);
		}

		// 캘린더 연동 상태 응답 반환
		String state = googleCalendarOAuthStateService.createState(user.getId());
		return googleCalendarOAuthClient.buildAuthorizationUrl(state);
	}

	/* Google Calendar OAuth callback 처리 */
	@Transactional
	public CalendarStatusResponse completeGoogleConnect(String code, String state) {
		Long userId = googleCalendarOAuthStateService.verifyAndExtractUserId(state);
		User user = findActiveGoogleUserById(userId);

		// 토큰 응답 유효성 검증
		GoogleOAuthTokenResponse tokenResponse = googleCalendarOAuthClient.exchangeAuthorizationCode(code);
		validateTokenResponse(tokenResponse);

		// 토큰 저장 또는 갱신
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plusSeconds(tokenResponse.expiresIn());
		saveOrUpdateToken(user, tokenResponse, expiresAt, now);

		return buildStatusForUser(user);
	}

	/* Google Calendar 연동 해제 */
	@Transactional
	public CalendarStatusResponse disconnectGoogle() {
		User user = findActiveGoogleUser();

		GoogleCalendarToken token = googleCalendarTokenRepository.findByUser_Id(user.getId())
			.filter(GoogleCalendarToken::isActive)
			.orElseThrow(() -> new BootSignalException(ErrorCode.CALENDAR_NOT_CONNECTED));

		token.revoke(LocalDateTime.now());
		return buildStatusForUser(user);
	}

	/* 성공 리다이렉트 URL 반환 */
	public String connectSuccessRedirectUrl() {
		return googleCalendarProperties.connectSuccessUrl();
	}

	/* 실패 리다이렉트 URL 반환 */
	public String connectFailureRedirectUrl() {
		return googleCalendarProperties.connectFailureUrl();
	}


	// 구글 캘린더 연동 상태 응답 빌드
	private CalendarStatusResponse buildStatusForUser(User user) {
		if (user.getProvider() != AuthProvider.GOOGLE) {
			return CalendarStatusResponse.notGoogleUser();
		}

		return googleCalendarTokenRepository.findByUser_Id(user.getId())
			.map(token -> {
				if (token.isActive()) {
					return CalendarStatusResponse.connected(token.getConnectedAt(), token.getExpiresAt());
				}
				return CalendarStatusResponse.notConnected();
			})
			.orElseGet(CalendarStatusResponse::notConnected);
	}

	// 구글 캘린더 토큰 저장 또는 갱신
	private void saveOrUpdateToken(
		User user,
		GoogleOAuthTokenResponse tokenResponse,
		LocalDateTime expiresAt,
		LocalDateTime connectedAt
	) {
		String scope = resolveScope(tokenResponse);

		// 토큰 암호화
		String encryptedAccessToken = tokenEncryptionService.encrypt(tokenResponse.accessToken());
		String encryptedRefreshToken = tokenEncryptionService.encrypt(tokenResponse.refreshToken());

		googleCalendarTokenRepository.findByUser_Id(user.getId())
			.ifPresentOrElse(
				token -> token.reconnect(
					encryptedAccessToken,
					encryptedRefreshToken,
					scope,
					expiresAt,
					connectedAt
				),
				() -> googleCalendarTokenRepository.save(GoogleCalendarToken.connect(
					user,
					encryptedAccessToken,
					encryptedRefreshToken,
					scope,
					expiresAt,
					connectedAt
				))
			);
	}

	// 구글 캘린더 토큰 응답 유효성 검증
	private void validateTokenResponse(GoogleOAuthTokenResponse tokenResponse) {
		if (tokenResponse == null
			|| !StringUtils.hasText(tokenResponse.accessToken())
			|| !StringUtils.hasText(tokenResponse.refreshToken())) {
			throw new BootSignalException(ErrorCode.CALENDAR_OAUTH_EXCHANGE_FAILED);
		}
	}

	// 구글 캘린더 토큰 접근 범위 해석
	private String resolveScope(GoogleOAuthTokenResponse tokenResponse) {
		return StringUtils.hasText(tokenResponse.scope())
			? tokenResponse.scope().strip()
			: googleCalendarProperties.scope();
	}

	// 구글 캘린더 사용자 유효성 검증
	private void validateGoogleUser(User user) {
		if (user.getProvider() != AuthProvider.GOOGLE) {
			throw new BootSignalException(ErrorCode.CALENDAR_GOOGLE_USER_ONLY);
		}
	}
	
	// 구글 캘린더 활성 사용자 조회 (ID 기반)
	private User findActiveGoogleUserById(Long userId) {
		User user = userRepository.findById(userId)
			.filter(activeUser -> !activeUser.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.USER_NOT_FOUND));
		validateGoogleUser(user);
		return user;
	}
	
	// 구글 로그인 사용자 조회
	private User findActiveGoogleUser() {
		User user = findActiveUser();
		validateGoogleUser(user);
		return user;
	}

	// 현재 사용자 조회
	private User findActiveUser() {
		String email = SecurityUtil.getCurrentUserEmail();

		return userRepository.findByEmail(email)
			.filter(activeUser -> !activeUser.isDeleted())
			.orElseThrow(() -> new BootSignalException(ErrorCode.USER_NOT_FOUND));
	}
}

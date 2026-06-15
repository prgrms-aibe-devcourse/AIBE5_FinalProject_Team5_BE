package com.bootsignal.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.calendar.client.GoogleCalendarOAuthClient;
import com.bootsignal.domain.calendar.client.dto.GoogleOAuthTokenResponse;
import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.config.properties.GoogleCalendarProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import com.bootsignal.global.security.crypto.TokenEncryptionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarService 테스트")
class CalendarServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private GoogleCalendarTokenRepository googleCalendarTokenRepository;

	@Mock
	private GoogleCalendarOAuthClient googleCalendarOAuthClient;

	@Mock
	private GoogleCalendarOAuthStateService googleCalendarOAuthStateService;

	@Mock
	private GoogleCalendarProperties googleCalendarProperties;

	@Mock
	private TokenEncryptionService tokenEncryptionService;

	@InjectMocks
	private CalendarService calendarService;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("구글 사용자 + 활성 토큰 있음 → connected true")
	void getStatusReturnsConnectedForGoogleUserWithActiveToken() {
		// given — 구글 로그인 사용자 + revoked_at IS NULL 토큰
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken token = activeToken(user);

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L))
			.willReturn(Optional.of(token));
		given(token.isActive()).willReturn(true);

		// when
		CalendarStatusResponse response = calendarService.getStatus();

		// then
		assertThat(response.connected()).isTrue();
		assertThat(response.googleUser()).isTrue();
		assertThat(response.connectedAt()).isEqualTo(token.getConnectedAt());
		assertThat(response.expiresAt()).isEqualTo(token.getExpiresAt());
	}

	@Test
	@DisplayName("구글 사용자 + 활성 토큰 없음 → connected false, googleUser true")
	void getStatusReturnsNotConnectedForGoogleUserWithoutToken() {
		// given — 구글 로그인 사용자 + 토큰 row 없음
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L))
			.willReturn(Optional.empty());

		// when
		CalendarStatusResponse response = calendarService.getStatus();

		// then
		assertThat(response.connected()).isFalse();
		assertThat(response.googleUser()).isTrue();
		assertThat(response.connectedAt()).isNull();
		assertThat(response.expiresAt()).isNull();
	}

	@Test
	@DisplayName("구글 사용자 + 해제된 토큰 → connected false, connectedAt/expiresAt null")
	void getStatusReturnsDisconnectedForRevokedToken() {
		// given — 구글 로그인 사용자 + revoked_at이 설정된 토큰
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken token = GoogleCalendarToken.connect(
			user,
			"access",
			"refresh",
			"calendar.events",
			LocalDateTime.of(2026, 6, 11, 12, 0),
			LocalDateTime.of(2026, 6, 11, 10, 0)
		);
		LocalDateTime revokedAt = LocalDateTime.of(2026, 6, 11, 13, 0);
		token.revoke(revokedAt);

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L))
			.willReturn(Optional.of(token));

		// when
		CalendarStatusResponse response = calendarService.getStatus();

		// then
		assertThat(response.connected()).isFalse();
		assertThat(response.googleUser()).isTrue();
		assertThat(response.connectedAt()).isNull();
		assertThat(response.expiresAt()).isNull();
	}

	@Test
	@DisplayName("로컬 사용자 → connected false, googleUser false")
	void getStatusReturnsNotGoogleUserForLocalUser() {
		// given — LOCAL 가입 사용자
		setAuthenticatedUser("user@example.com");
		User user = localUser(1L, "user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		// when
		CalendarStatusResponse response = calendarService.getStatus();

		// then
		assertThat(response.connected()).isFalse();
		assertThat(response.googleUser()).isFalse();
	}

	@Test
	@DisplayName("미로그인 → UNAUTHORIZED")
	void getStatusThrowsWhenNotAuthenticated() {
		// given — SecurityContext에 인증 정보 없음

		// when & then
		assertThatThrownBy(() -> calendarService.getStatus())
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("구글 사용자 연동 시작 → Google OAuth URL 반환")
	void startGoogleConnectReturnsAuthorizationUrl() {
		// given — 미연동 구글 사용자
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.empty());
		given(googleCalendarOAuthStateService.createState(1L)).willReturn("oauth-state");
		given(googleCalendarOAuthClient.buildAuthorizationUrl("oauth-state"))
			.willReturn("https://accounts.google.com/o/oauth2/v2/auth?state=oauth-state");

		// when
		String authorizationUrl = calendarService.startGoogleConnect();

		// then
		assertThat(authorizationUrl).contains("accounts.google.com");
	}

	@Test
	@DisplayName("이미 연동된 구글 사용자 → CALENDAR_ALREADY_CONNECTED")
	void startGoogleConnectThrowsWhenAlreadyConnected() {
		// given — 활성 토큰이 있는 구글 사용자
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken token = GoogleCalendarToken.connect(
			user,
			"access",
			"refresh",
			"calendar.events",
			LocalDateTime.of(2026, 6, 11, 12, 0),
			LocalDateTime.of(2026, 6, 11, 10, 0)
		);

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));

		// when & then
		assertThatThrownBy(() -> calendarService.startGoogleConnect())
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CALENDAR_ALREADY_CONNECTED);
	}

	@Test
	@DisplayName("OAuth callback 성공 → 연동 상태 반환 및 토큰 저장")
	void completeGoogleConnectSavesTokenAndReturnsConnectedStatus() {
		// given
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken savedToken = GoogleCalendarToken.connect(
			user,
			"access-token",
			"refresh-token",
			"calendar.events",
			LocalDateTime.of(2026, 6, 14, 15, 0),
			LocalDateTime.of(2026, 6, 14, 14, 0)
		);
		GoogleOAuthTokenResponse tokenResponse = new GoogleOAuthTokenResponse(
			"access-token",
			3600L,
			"refresh-token",
			"calendar.events",
			"Bearer"
		);

		given(googleCalendarOAuthStateService.verifyAndExtractUserId("state")).willReturn(1L);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L))
			.willReturn(Optional.empty(), Optional.of(savedToken));
		given(googleCalendarOAuthClient.exchangeAuthorizationCode("auth-code")).willReturn(tokenResponse);
		given(tokenEncryptionService.encrypt("access-token")).willReturn("v1:encrypted-access");
		given(tokenEncryptionService.encrypt("refresh-token")).willReturn("v1:encrypted-refresh");

		// when
		CalendarStatusResponse response = calendarService.completeGoogleConnect("auth-code", "state");

		// then
		assertThat(response.connected()).isTrue();
		assertThat(response.googleUser()).isTrue();
		verify(googleCalendarTokenRepository).save(any(GoogleCalendarToken.class));
	}

	@Test
	@DisplayName("OAuth callback — 기존 row UPDATE reconnect")
	void completeGoogleConnectUpdatesExistingToken() {
		// given — 해제된 토큰 row가 있는 사용자
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken existingToken = GoogleCalendarToken.connect(
			user,
			"old-access",
			"old-refresh",
			"calendar.events",
			LocalDateTime.of(2026, 6, 11, 10, 0),
			LocalDateTime.of(2026, 6, 11, 9, 0)
		);
		ReflectionTestUtils.setField(existingToken, "revokedAt", LocalDateTime.of(2026, 6, 11, 11, 0));

		GoogleOAuthTokenResponse tokenResponse = new GoogleOAuthTokenResponse(
			"new-access",
			3600L,
			"new-refresh",
			"calendar.events",
			"Bearer"
		);

		given(googleCalendarOAuthStateService.verifyAndExtractUserId("state")).willReturn(1L);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(existingToken));
		given(googleCalendarOAuthClient.exchangeAuthorizationCode("auth-code")).willReturn(tokenResponse);
		given(tokenEncryptionService.encrypt("new-access")).willReturn("v1:encrypted-new-access");
		given(tokenEncryptionService.encrypt("new-refresh")).willReturn("v1:encrypted-new-refresh");

		// when
		calendarService.completeGoogleConnect("auth-code", "state");

		// then
		assertThat(existingToken.getAccessTokenEncrypted()).isEqualTo("v1:encrypted-new-access");
		assertThat(existingToken.getRefreshTokenEncrypted()).isEqualTo("v1:encrypted-new-refresh");
		assertThat(existingToken.isActive()).isTrue();
		verify(googleCalendarTokenRepository, never()).save(any(GoogleCalendarToken.class));
	}

	@Test
	@DisplayName("연동 해제 — revoked_at 설정")
	void disconnectGoogleRevokesActiveToken() {
		// given — 활성 토큰이 있는 구글 사용자
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken token = GoogleCalendarToken.connect(
			user,
			"access",
			"refresh",
			"calendar.events",
			LocalDateTime.of(2026, 6, 11, 12, 0),
			LocalDateTime.of(2026, 6, 11, 10, 0)
		);

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.of(token));

		// when
		CalendarStatusResponse response = calendarService.disconnectGoogle();

		// then
		assertThat(token.isActive()).isFalse();
		assertThat(token.getRevokedAt()).isNotNull();
		assertThat(response.connected()).isFalse();
		assertThat(response.googleUser()).isTrue();
		assertThat(response.connectedAt()).isNull();
		assertThat(response.expiresAt()).isNull();
	}

	@Test
	@DisplayName("미연동 구글 사용자 연동 해제 → CALENDAR_NOT_CONNECTED")
	void disconnectGoogleThrowsWhenNotConnected() {
		// given — 토큰 row 없음
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> calendarService.disconnectGoogle())
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CALENDAR_NOT_CONNECTED);
	}

	@Test
	@DisplayName("로컬 사용자 연동 해제 → CALENDAR_GOOGLE_USER_ONLY")
	void disconnectGoogleThrowsForLocalUser() {
		// given
		setAuthenticatedUser("user@example.com");
		User user = localUser(1L, "user@example.com");

		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

		// when & then
		assertThatThrownBy(() -> calendarService.disconnectGoogle())
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CALENDAR_GOOGLE_USER_ONLY);
	}

	private void setAuthenticatedUser(String email) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(
				email,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))
			)
		);
	}

	private User googleUser(Long id, String email) {
		User user = User.signupGoogle(email, "google-sub", "테스트", "테스트", null);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private User localUser(Long id, String email) {
		User user = User.signupLocal(email, "encoded-password", "테스트");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private GoogleCalendarToken activeToken(User user) {
		LocalDateTime connectedAt = LocalDateTime.of(2026, 6, 11, 10, 0);
		LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 11, 12, 0);
		GoogleCalendarToken token = mock(GoogleCalendarToken.class);
		given(token.getConnectedAt()).willReturn(connectedAt);
		given(token.getExpiresAt()).willReturn(expiresAt);
		return token;
	}
}

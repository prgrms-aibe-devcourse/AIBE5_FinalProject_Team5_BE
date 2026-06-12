package com.bootsignal.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.bootsignal.domain.calendar.dto.CalendarStatusResponse;
import com.bootsignal.domain.calendar.entity.GoogleCalendarToken;
import com.bootsignal.domain.calendar.repository.GoogleCalendarTokenRepository;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
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
	@DisplayName("구글 사용자 + 해제된 토큰 → connected false, googleUser true")
	void getStatusReturnsNotConnectedForRevokedToken() {
		// given — 구글 로그인 사용자 + revoked_at이 설정된 토큰
		setAuthenticatedUser("user@gmail.com");
		User user = googleUser(1L, "user@gmail.com");
		GoogleCalendarToken token = revokedToken();

		given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(user));
		given(googleCalendarTokenRepository.findByUser_Id(1L))
			.willReturn(Optional.of(token));
		given(token.isActive()).willReturn(false);

		// when
		CalendarStatusResponse response = calendarService.getStatus();

		// then
		assertThat(response.connected()).isFalse();
		assertThat(response.googleUser()).isTrue();
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

	private GoogleCalendarToken revokedToken() {
		return mock(GoogleCalendarToken.class);
	}
}

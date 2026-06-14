package com.bootsignal.domain.calendar.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.user.entity.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("GoogleCalendarToken 테스트")
class GoogleCalendarTokenTest {

	private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 6, 11, 10, 0);
	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 6, 11, 12, 0);
	private static final LocalDateTime REVOKED_AT = LocalDateTime.of(2026, 6, 12, 10, 0);

	@Test
	@DisplayName("connect — revoked_at이 null인 활성 토큰 생성")
	void connectCreatesActiveToken() {
		// given
		User user = User.signupGoogle("user@gmail.com", "sub", "테스트", "테스트", null);

		// when
		GoogleCalendarToken token = GoogleCalendarToken.connect(
			user,
			"access",
			"refresh",
			"calendar.events",
			EXPIRES_AT,
			CONNECTED_AT
		);

		// then
		assertThat(token.isActive()).isTrue();
		assertThat(token.getRevokedAt()).isNull();
		assertThat(token.getConnectedAt()).isEqualTo(CONNECTED_AT);
	}

	@Test
	@DisplayName("reconnect — 기존 row를 갱신하고 revoked_at을 null로 되돌림")
	void reconnectReactivatesRevokedToken() {
		// given — disconnect 처리된 토큰
		User user = User.signupGoogle("user@gmail.com", "sub", "테스트", "테스트", null);
		GoogleCalendarToken token = GoogleCalendarToken.connect(
			user, "old-access", "old-refresh", "calendar.events", EXPIRES_AT, CONNECTED_AT
		);
		ReflectionTestUtils.setField(token, "revokedAt", REVOKED_AT);
		LocalDateTime newConnectedAt = LocalDateTime.of(2026, 6, 13, 9, 0);
		LocalDateTime newExpiresAt = LocalDateTime.of(2026, 6, 13, 11, 0);

		// when
		token.reconnect("new-access", "new-refresh", "calendar.events", newExpiresAt, newConnectedAt);

		// then
		assertThat(token.isActive()).isTrue();
		assertThat(token.getRevokedAt()).isNull();
		assertThat(token.getAccessTokenEncrypted()).isEqualTo("new-access");
		assertThat(token.getRefreshTokenEncrypted()).isEqualTo("new-refresh");
		assertThat(token.getConnectedAt()).isEqualTo(newConnectedAt);
	}

	@Test
	@DisplayName("revoked_at이 null이면 활성 연동")
	void isActiveReturnsTrueWhenRevokedAtIsNull() {
		// given
		GoogleCalendarToken token = new GoogleCalendarToken();

		// when & then
		assertThat(token.isActive()).isTrue();
	}

	@Test
	@DisplayName("revoked_at이 있으면 비활성 연동")
	void isActiveReturnsFalseWhenRevokedAtIsSet() {
		// given
		GoogleCalendarToken token = new GoogleCalendarToken();
		ReflectionTestUtils.setField(token, "revokedAt", REVOKED_AT);

		// when & then
		assertThat(token.isActive()).isFalse();
	}

	@Test
	@DisplayName("revoke — revoked_at 설정 시 비활성 상태")
	void revokeMarksTokenInactive() {
		// given
		User user = User.signupGoogle("user@gmail.com", "sub", "테스트", "테스트", null);
		GoogleCalendarToken token = GoogleCalendarToken.connect(
			user, "access", "refresh", "calendar.events", EXPIRES_AT, CONNECTED_AT
		);

		// when
		token.revoke(REVOKED_AT);

		// then
		assertThat(token.isActive()).isFalse();
		assertThat(token.getRevokedAt()).isEqualTo(REVOKED_AT);
	}
}

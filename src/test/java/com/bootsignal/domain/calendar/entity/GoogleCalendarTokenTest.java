package com.bootsignal.domain.calendar.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("GoogleCalendarToken 테스트")
class GoogleCalendarTokenTest {

	@Test
	@DisplayName("revoked_at이 null이면 활성 연동")
	void isActiveReturnsTrueWhenRevokedAtIsNull() {
		// given — revoked_at이 설정되지 않은 토큰
		GoogleCalendarToken token = new GoogleCalendarToken();

		// when & then
		assertThat(token.isActive()).isTrue();
	}

	@Test
	@DisplayName("revoked_at이 있으면 비활성 연동")
	void isActiveReturnsFalseWhenRevokedAtIsSet() {
		// given — disconnect 처리된 토큰
		GoogleCalendarToken token = new GoogleCalendarToken();
		ReflectionTestUtils.setField(token, "revokedAt", LocalDateTime.of(2026, 6, 12, 10, 0));

		// when & then
		assertThat(token.isActive()).isFalse();
	}
}

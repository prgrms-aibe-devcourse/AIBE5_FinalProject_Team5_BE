package com.bootsignal.domain.auth.service;

import com.bootsignal.domain.user.entity.User;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 메일 발송 인프라가 연결되기 전까지 원문 토큰을 노출하지 않고 발급 사실만 남기는 기본 알림 구현입니다.
 */
@Slf4j
@Component
public class NoopPasswordResetTokenNotifier implements PasswordResetTokenNotifier {

	@Override
	public void send(User user, String rawToken, Instant expiresAt) {
		// 보안상 rawToken은 API 응답이나 로그에 남기지 않는다.
		log.info("Password reset token issued. userId={}, expiresAt={}", user.getId(), expiresAt);
	}
}

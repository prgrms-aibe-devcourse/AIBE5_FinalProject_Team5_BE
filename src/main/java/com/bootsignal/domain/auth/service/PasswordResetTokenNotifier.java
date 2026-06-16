package com.bootsignal.domain.auth.service;

import com.bootsignal.domain.user.entity.User;
import java.time.Instant;

/**
 * 비밀번호 재설정 원문 토큰을 이메일 등 외부 채널로 전달하기 위한 확장 지점입니다.
 */
public interface PasswordResetTokenNotifier {

	void send(User user, String rawToken, Instant expiresAt);
}

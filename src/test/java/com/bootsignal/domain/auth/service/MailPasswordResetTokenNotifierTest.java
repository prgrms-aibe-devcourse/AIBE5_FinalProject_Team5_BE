package com.bootsignal.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.config.properties.PasswordResetProperties;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 메일 기반 비밀번호 재설정 알림이 사용자에게 재설정 링크를 전달하는지 검증하는 단위 테스트입니다.
 */
class MailPasswordResetTokenNotifierTest {

	@Test
	void sendBuildsPasswordResetMailWithResetUrl() {
		JavaMailSender javaMailSender = org.mockito.Mockito.mock(JavaMailSender.class);
		PasswordResetProperties properties = new PasswordResetProperties(
			1800L,
			false,
			"http://localhost:5173/reset-password?token={token}",
			new PasswordResetProperties.Mail(true, "support@bootsignal.com", "비밀번호 재설정")
		);
		MailPasswordResetTokenNotifier notifier = new MailPasswordResetTokenNotifier(javaMailSender, properties);
		User user = User.signupLocal("user@example.com", "encoded-password", "홍길동", "tester");
		ReflectionTestUtils.setField(user, "id", 1L);

		notifier.send(
			user,
			"raw-reset-token",
			"http://localhost:5173/reset-password?token=raw-reset-token",
			Instant.parse("2026-06-16T05:00:00Z")
		);

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(javaMailSender).send(captor.capture());
		SimpleMailMessage message = captor.getValue();
		assertThat(message.getFrom()).isEqualTo("support@bootsignal.com");
		assertThat(message.getTo()).containsExactly("user@example.com");
		assertThat(message.getSubject()).isEqualTo("비밀번호 재설정");
		assertThat(message.getText())
			.contains("http://localhost:5173/reset-password?token=raw-reset-token")
			.contains("2026-06-16T05:00:00Z");
	}
}

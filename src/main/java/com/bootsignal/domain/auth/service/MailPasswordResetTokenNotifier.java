package com.bootsignal.domain.auth.service;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.config.properties.PasswordResetProperties;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 재설정 링크를 사용자 이메일로 발송하는 운영용 알림 구현입니다.
 */
@Component
@RequiredArgsConstructor
@Primary
@ConditionalOnProperty(prefix = "app.auth.password-reset.mail", name = "enabled", havingValue = "true")
public class MailPasswordResetTokenNotifier implements PasswordResetTokenNotifier {

	private final JavaMailSender javaMailSender;
	private final PasswordResetProperties passwordResetProperties;

	@Override
	public void send(User user, String rawToken, String resetUrl, Instant expiresAt) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(passwordResetProperties.mail().from());
		message.setTo(user.getEmail());
		message.setSubject(passwordResetProperties.mail().subject());
		message.setText("""
			BootSignal 비밀번호 재설정을 요청하셨습니다.

			아래 링크에서 새 비밀번호를 설정해 주세요.
			%s

			이 링크는 %s까지 사용할 수 있습니다.
			본인이 요청하지 않았다면 이 메일을 무시해 주세요.
			""".formatted(resetUrl, expiresAt));

		javaMailSender.send(message);
	}
}

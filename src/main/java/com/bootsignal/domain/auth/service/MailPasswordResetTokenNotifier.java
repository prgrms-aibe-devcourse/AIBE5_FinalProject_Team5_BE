package com.bootsignal.domain.auth.service;

import com.bootsignal.domain.user.entity.User;
import com.bootsignal.global.config.Utf8DotenvLoader;
import com.bootsignal.global.config.properties.PasswordResetProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 재설정 링크를 사용자 이메일로 발송하는 운영용 알림 구현입니다.
 */
@Component
@RequiredArgsConstructor
@Primary
@ConditionalOnProperty(prefix = "app.auth.password-reset.mail", name = "enabled", havingValue = "true")
public class MailPasswordResetTokenNotifier implements PasswordResetTokenNotifier {

	private static final String MAIL_SUBJECT_ENV = "PASSWORD_RESET_MAIL_SUBJECT";

	private final JavaMailSender javaMailSender;
	private final PasswordResetProperties passwordResetProperties;

	@Override
	public void send(User user, String rawToken, String resetUrl, Instant expiresAt) {
		try {
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
			helper.setFrom(passwordResetProperties.mail().from());
			helper.setTo(user.getEmail());
			helper.setSubject(resolveSubject());
			helper.setText("""
				BootSignal 비밀번호 재설정을 요청하셨습니다.

				아래 링크에서 새 비밀번호를 설정해 주세요.
				%s

				이 링크는 %s까지 사용할 수 있습니다.
				본인이 요청하지 않았다면 이 메일을 무시해 주세요.
				""".formatted(resetUrl, expiresAt));
			javaMailSender.send(mimeMessage);
		} catch (MessagingException exception) {
			throw new IllegalStateException("비밀번호 재설정 메일 발송에 실패했습니다.", exception);
		}
	}

	private String resolveSubject() {
		String fromDotenv = Utf8DotenvLoader.get(MAIL_SUBJECT_ENV);
		if (fromDotenv != null && !fromDotenv.isBlank()) {
			return fromDotenv;
		}
		return Utf8DotenvLoader.fixUtf8MisreadAsIso8859(passwordResetProperties.mail().subject());
	}
}

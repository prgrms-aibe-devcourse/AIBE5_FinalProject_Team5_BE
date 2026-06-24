package com.bootsignal.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Utf8DotenvLoaderTest {

	@Test
	void loadsPasswordResetMailSubjectWithHangulFromProjectEnv() {
		Path envPath = Path.of(".env");
		if (!Files.isRegularFile(envPath)) {
			return;
		}

		String subject = Utf8DotenvLoader.get("PASSWORD_RESET_MAIL_SUBJECT");
		assertThat(subject).isNotNull();
		assertThat(subject).contains("BootSignal");
		assertThat(subject.chars().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HANGUL))
			.as("subject should contain Hangul when .env is UTF-8: %s", subject)
			.isTrue();
	}

	@Test
	void simulatesIso8859MisreadAndFixesMojibake() {
		String original = "[BootSignal] 비밀번호 재설정 안내";
		String corrupted = new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

		String fixed = Utf8DotenvLoader.fixUtf8MisreadAsIso8859(corrupted);

		assertThat(fixed).isEqualTo(original);
	}
}

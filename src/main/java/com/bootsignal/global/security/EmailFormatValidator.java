package com.bootsignal.global.security;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class EmailFormatValidator {

	private static final Pattern EMAIL_PATTERN = Pattern.compile(
		"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
		Pattern.CASE_INSENSITIVE
	);

	private EmailFormatValidator() {
	}

	public static String normalize(String email) {
		return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
	}

	public static boolean isValid(String email) {
		String normalizedEmail = normalize(email);
		return StringUtils.hasText(normalizedEmail) && EMAIL_PATTERN.matcher(normalizedEmail).matches();
	}
}

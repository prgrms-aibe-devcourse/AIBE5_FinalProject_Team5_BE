package com.bootsignal.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * .env 파일을 UTF-8로 직접 파싱합니다. Spring config import의 ISO-8859-1 한계를 우회합니다.
 */
public final class Utf8DotenvLoader {

	private static final List<Path> DOTENV_CANDIDATES = List.of(
		Path.of(".env"),
		Path.of(System.getProperty("user.dir", "."), ".env")
	);

	private Utf8DotenvLoader() {
	}

	public static Map<String, String> load() {
		for (Path candidate : DOTENV_CANDIDATES) {
			Path normalized = candidate.toAbsolutePath().normalize();
			if (!Files.isRegularFile(normalized)) {
				continue;
			}
			try {
				return parse(normalized);
			} catch (IOException ignored) {
				// try next candidate
			}
		}
		return Map.of();
	}

	public static String get(String key) {
		return load().get(key);
	}

	static Map<String, String> parse(Path path) throws IOException {
		Map<String, String> values = new HashMap<>();
		for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
			String trimmed = stripBom(line.strip());
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			int separator = trimmed.indexOf('=');
			if (separator <= 0) {
				continue;
			}
			String key = trimmed.substring(0, separator).strip();
			String value = trimmed.substring(separator + 1).strip();
			if (!key.isEmpty()) {
				values.put(key, value);
			}
		}
		return values;
	}

	public static String fixUtf8MisreadAsIso8859(String value) {
		if (value == null || value.isBlank() || containsHangul(value)) {
			return value;
		}
		if (value.chars().noneMatch(ch -> ch > 127)) {
			return value;
		}
		String fixed = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		return containsHangul(fixed) ? fixed : value;
	}

	private static boolean containsHangul(String value) {
		return value.chars().anyMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HANGUL);
	}

	private static String stripBom(String line) {
		if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
			return line.substring(1);
		}
		return line;
	}
}

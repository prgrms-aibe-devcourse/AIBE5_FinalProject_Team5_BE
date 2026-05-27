package com.bootsignal.domain.work24.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.work24.crawler")
public record Work24CrawlerProperties(
	@NotBlank String defaultCourseUrl,
	@NotBlank String outputPath,
	int timeoutMillis,
	@NotBlank String userAgent
) {

	private static final String DEFAULT_COURSE_URL = "https://www.work24.go.kr/hr/a/a/3100/selectTracseDetl.do"
		+ "?tracseId=AIG20250000501645&tracseTme=4&crseTracseSe=C0061&trainstCstmrId=500020021537";
	private static final String DEFAULT_OUTPUT_PATH = "build/crawled/work24-training-course-overview.json";
	private static final int DEFAULT_TIMEOUT_MILLIS = 10_000;
	private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 BootSignalCrawler/1.0";

	public Work24CrawlerProperties {
		defaultCourseUrl = defaultIfBlank(defaultCourseUrl, DEFAULT_COURSE_URL);
		outputPath = defaultIfBlank(outputPath, DEFAULT_OUTPUT_PATH);
		timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
		userAgent = defaultIfBlank(userAgent, DEFAULT_USER_AGENT);
	}

	private static String defaultIfBlank(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}
}

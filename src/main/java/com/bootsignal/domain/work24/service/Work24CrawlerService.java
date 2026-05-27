package com.bootsignal.domain.work24.service;

import com.bootsignal.domain.work24.config.Work24CrawlerProperties;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverview;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewCrawlRequest;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewSaveResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Work24CrawlerService {

	private static final String OVERVIEW_TABLE_SELECTOR = "#traCrseinfo table";
	private static final String TARGET_REQUIREMENTS_LABEL = "훈련대상자요건";
	private static final String TRAINING_GOAL_LABEL = "훈련목표";

	private final ObjectMapper objectMapper;
	private final Work24CrawlerProperties properties;

	public Work24TrainingCourseOverviewSaveResult crawlAndSave(Work24TrainingCourseOverviewCrawlRequest request)
		throws IOException {
		String sourceUrl = resolveSourceUrl(request);
		Document document = Jsoup.connect(sourceUrl)
			.userAgent(properties.userAgent())
			.timeout(properties.timeoutMillis())
			.get();

		Work24TrainingCourseOverview overview = parse(document, sourceUrl, Instant.now(Clock.systemUTC()));
		Path savedPath = save(overview, resolveOutputPath(request));
		return new Work24TrainingCourseOverviewSaveResult(overview, savedPath.toString());
	}

	public Work24TrainingCourseOverview parse(Document document, String sourceUrl, Instant crawledAt) {
		Element overviewTable = document.selectFirst(OVERVIEW_TABLE_SELECTOR);
		if (overviewTable == null) {
			throw new IllegalStateException("훈련과정 개요 테이블을 찾을 수 없습니다.");
		}

		return new Work24TrainingCourseOverview(
			sourceUrl,
			extractTextByHeader(overviewTable, TARGET_REQUIREMENTS_LABEL),
			extractTextByHeader(overviewTable, TRAINING_GOAL_LABEL),
			crawledAt
		);
	}

	public Path save(Work24TrainingCourseOverview overview, Path outputPath) throws IOException {
		Path absoluteOutputPath = outputPath.toAbsolutePath().normalize();
		Path parent = absoluteOutputPath.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(absoluteOutputPath.toFile(), overview);
		return absoluteOutputPath;
	}

	private String extractTextByHeader(Element table, String headerText) {
		String expectedHeader = compact(headerText);
		return table.select("tr").stream()
			.filter(row -> row.select("th").stream()
				.map(Element::text)
				.map(this::compact)
				.anyMatch(expectedHeader::equals))
			.map(row -> row.selectFirst("td"))
			.filter(Objects::nonNull)
			.map(this::extractMultilineText)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(headerText + " 항목을 찾을 수 없습니다."));
	}

	private String extractMultilineText(Element cell) {
		Element copy = cell.clone();
		copy.select("br").forEach(br -> {
			br.before(new TextNode("\n"));
			br.remove();
		});

		return normalizeMultilineText(copy.wholeText());
	}

	private String normalizeMultilineText(String text) {
		String normalized = text
			.replace('\u00A0', ' ')
			.replace("\r", "")
			.replaceAll("[ \\t]+", " ");
		normalized = normalized.lines()
			.map(String::trim)
			.reduce((left, right) -> left + "\n" + right)
			.orElse("");
		return normalized.replaceAll("\\n{3,}", "\n\n").trim();
	}

	private String compact(String text) {
		return text.replaceAll("\\s+", "");
	}

	private String resolveSourceUrl(Work24TrainingCourseOverviewCrawlRequest request) {
		String url = request == null || request.url() == null || request.url().isBlank()
			? properties.defaultCourseUrl()
			: request.url();
		URI uri = URI.create(url);
		if (uri.getScheme() == null || uri.getHost() == null) {
			throw new IllegalArgumentException("크롤링 URL 형식이 올바르지 않습니다.");
		}
		return uri.toString();
	}

	private Path resolveOutputPath(Work24TrainingCourseOverviewCrawlRequest request) {
		String outputPath = request == null || request.outputPath() == null || request.outputPath().isBlank()
			? properties.outputPath()
			: request.outputPath();
		return Path.of(outputPath);
	}
}

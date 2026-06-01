package com.bootsignal.domain.work24.service;

import com.bootsignal.domain.work24.config.Work24CrawlerProperties;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverview;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewCrawlRequest;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverviewSaveResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	private static final String INSTITUTION_INFO_PATH = "/hr/a/a/3200/selectTrainInstitutionPost.do";
	private static final String TARGET_REQUIREMENTS_LABEL = "훈련대상자요건";
	private static final String TRAINING_GOAL_LABEL = "훈련목표";
	private static final String CONFIRMED_TRAINEE_COUNT_LABEL = "수강확정인원";
	private static final String SELECTED_TRAINEE_COUNT_LABEL = "선발인원";
	private static final String RECRUITMENT_COUNT_LABEL = "모집인원";
	private static final String INSTITUTION_INTRODUCTION_LABEL = "훈련기관 소개";
	private static final Pattern INTEGER_PATTERN = Pattern.compile("([\\d,]+)");
	private static final Pattern EMPLOYMENT_RATE_SCRIPT_PATTERN = Pattern.compile(
		"newEmpymnRt\\s*=\\s*['\"]([\\d]+(?:\\.\\d+)?)['\"]"
	);
	private static final Pattern DECIMAL_PATTERN = Pattern.compile("([\\d]+(?:\\.\\d+)?)");

	private final ObjectMapper objectMapper;
	private final Work24CrawlerProperties properties;

	public Work24TrainingCourseOverviewSaveResult crawlAndSave(Work24TrainingCourseOverviewCrawlRequest request)
		throws IOException {
		String sourceUrl = resolveSourceUrl(request);
		Document document = Jsoup.connect(sourceUrl)
			.userAgent(properties.userAgent())
			.timeout(properties.timeoutMillis())
			.get();

		// 훈련기관 정보는 상세 페이지의 탭 클릭 시 별도 POST 페이지로 내려온다.
		Document institutionDocument = Jsoup.connect(resolveInstitutionInfoUrl(sourceUrl))
			.userAgent(properties.userAgent())
			.timeout(properties.timeoutMillis())
			.referrer(sourceUrl)
			.data(resolveInstitutionRequestData(document, sourceUrl))
			.post();

		Work24TrainingCourseOverview overview = parse(
			document,
			institutionDocument,
			sourceUrl,
			Instant.now(Clock.systemUTC())
		);
		Path savedPath = save(overview, resolveOutputPath(request));
		return new Work24TrainingCourseOverviewSaveResult(overview, savedPath.toString());
	}

	public Work24TrainingCourseOverview parse(Document document, String sourceUrl, Instant crawledAt) {
		return parse(document, document, sourceUrl, crawledAt);
	}

	public Work24TrainingCourseOverview parse(
		Document document,
		Document institutionDocument,
		String sourceUrl,
		Instant crawledAt
	) {
		Element overviewTable = document.selectFirst(OVERVIEW_TABLE_SELECTOR);
		if (overviewTable == null) {
			throw new IllegalStateException("훈련과정 개요 테이블을 찾을 수 없습니다.");
		}

		// 과정 기본 정보와 기관 탭 정보를 하나의 JSON 저장 DTO로 합친다.
		return new Work24TrainingCourseOverview(
			sourceUrl,
			extractTextByHeader(overviewTable, TARGET_REQUIREMENTS_LABEL),
			extractOptionalTextByHeader(overviewTable, TRAINING_GOAL_LABEL),
			extractCountByLabel(document, CONFIRMED_TRAINEE_COUNT_LABEL),
			extractCountByLabel(document, SELECTED_TRAINEE_COUNT_LABEL),
			extractCountByLabel(document, RECRUITMENT_COUNT_LABEL),
			extractEmploymentRate(document),
			extractInstitutionProfileImageUrl(institutionDocument),
			extractInstitutionIntroduction(institutionDocument),
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
		String text = extractOptionalTextByHeader(table, headerText);
		if (text == null) {
			throw new IllegalStateException(headerText + " 항목을 찾을 수 없습니다.");
		}
		return text;
	}

	private String extractOptionalTextByHeader(Element table, String headerText) {
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
			.orElse(null);
	}

	private String extractMultilineText(Element cell) {
		Element copy = cell.clone();
		copy.select("br").forEach(br -> {
			br.before(new TextNode("\n"));
			br.remove();
		});

		return normalizeMultilineText(copy.wholeText());
	}

	private Integer extractCountByLabel(Document document, String label) {
		String expectedLabel = compact(label);
		return document.select(".etc_info .count").stream()
			.filter(element -> compact(element.text()).startsWith(expectedLabel))
			.map(Element::text)
			.map(this::extractInteger)
			.filter(Objects::nonNull)
			.findFirst()
			.orElseGet(() -> extractCountByLabelFromText(document.text(), label));
	}

	private Integer extractCountByLabelFromText(String text, String label) {
		Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*([\\d,]+)\\s*명").matcher(text);
		if (!matcher.find()) {
			return null;
		}
		return parseInteger(matcher.group(1));
	}

	private Integer extractInteger(String text) {
		Matcher matcher = INTEGER_PATTERN.matcher(text);
		if (!matcher.find()) {
			return null;
		}
		return parseInteger(matcher.group(1));
	}

	private Integer parseInteger(String value) {
		return Integer.valueOf(value.replace(",", ""));
	}

	private BigDecimal extractEmploymentRate(Document document) {
		// 화면의 취업률 그래프는 JS 실행 후 갱신되므로 원본 스크립트 값을 우선 사용한다.
		return document.select("script").stream()
			.map(Element::data)
			.map(EMPLOYMENT_RATE_SCRIPT_PATTERN::matcher)
			.filter(Matcher::find)
			.map(matcher -> new BigDecimal(matcher.group(1)))
			.findFirst()
			.orElseGet(() -> extractEmploymentRateFromGraph(document));
	}

	private BigDecimal extractEmploymentRateFromGraph(Document document) {
		Element graphBar = document.selectFirst(".graphBar .bar");
		if (graphBar == null) {
			return null;
		}
		Matcher matcher = DECIMAL_PATTERN.matcher(graphBar.text());
		if (!matcher.find()) {
			return null;
		}
		return new BigDecimal(matcher.group(1));
	}

	private String extractInstitutionProfileImageUrl(Document document) {
		Element image = document.selectFirst("img[title=훈련기관사진], img[alt=훈련기관사진]");
		if (image == null) {
			return null;
		}
		return resolveUrl(image, "src");
	}

	private String extractInstitutionIntroduction(Document document) {
		// 기관 소개 영역은 h3 제목 바로 다음 설명 박스에 배치된다.
		Element section = findContentAfterHeading(
			document,
			INSTITUTION_INTRODUCTION_LABEL,
			".addExplainBoxArea"
		);
		if (section == null) {
			section = document.selectFirst(".addExplainBoxArea");
		}
		if (section == null) {
			return null;
		}

		Element textElement = section.selectFirst("pre.txt, .txt, pre");
		Element introduction = textElement == null ? section : textElement;
		return normalizeMultilineText(introduction.wholeText());
	}

	private Element findContentAfterHeading(Document document, String headingText, String contentSelector) {
		String expectedHeading = compact(headingText);
		for (Element heading : document.select("h1,h2,h3,h4,h5,h6")) {
			if (!expectedHeading.equals(compact(heading.text()))) {
				continue;
			}

			Element sectionHeader = findAncestorWithClass(heading, "box_group_wrap");
			Element sibling = sectionHeader == null ? heading.nextElementSibling() : sectionHeader.nextElementSibling();
			while (sibling != null) {
				if (sibling.is(contentSelector)) {
					return sibling;
				}
				Element candidate = sibling.selectFirst(contentSelector);
				if (candidate != null) {
					return candidate;
				}
				if (!sibling.select("h1,h2,h3,h4,h5,h6").isEmpty()) {
					break;
				}
				sibling = sibling.nextElementSibling();
			}
		}
		return null;
	}

	private Element findAncestorWithClass(Element element, String className) {
		Element current = element;
		while (current != null) {
			if (current.hasClass(className)) {
				return current;
			}
			current = current.parent();
		}
		return null;
	}

	private String resolveUrl(Element element, String attributeKey) {
		String absoluteUrl = element.absUrl(attributeKey);
		if (!absoluteUrl.isBlank()) {
			return absoluteUrl;
		}
		String url = element.attr(attributeKey);
		return url.isBlank() ? null : url;
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
		return text == null ? "" : text.replaceAll("\\s+", "");
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

	private String resolveInstitutionInfoUrl(String sourceUrl) {
		return URI.create(sourceUrl).resolve(INSTITUTION_INFO_PATH).toString();
	}

	private Map<String, String> resolveInstitutionRequestData(Document document, String sourceUrl) {
		// 기관 탭 POST에는 상세 페이지 hidden input과 URL query의 식별자가 모두 필요하다.
		Map<String, String> queryParameters = queryParameters(sourceUrl);
		Map<String, String> requestData = new LinkedHashMap<>();
		putRequiredParameter(requestData, "tracseId", resolveParameter(document, queryParameters, "tracseId"));
		putRequiredParameter(requestData, "tracseTme", resolveParameter(document, queryParameters, "tracseTme"));
		putRequiredParameter(
			requestData,
			"trainstCstmrId",
			resolveParameter(document, queryParameters, "trainstCstmrId")
		);
		putRequiredParameter(requestData, "crseTracseSe", resolveParameter(document, queryParameters, "crseTracseSe"));
		putOptionalParameter(requestData, "mainTracseSe", resolveParameter(document, queryParameters, "mainTracseSe"));
		return requestData;
	}

	private String resolveParameter(Document document, Map<String, String> queryParameters, String key) {
		return firstNonBlank(inputValue(document, key), queryParameters.get(key));
	}

	private String inputValue(Document document, String key) {
		Element input = document.selectFirst("input[name=" + key + "], input#" + key);
		return input == null ? null : input.attr("value");
	}

	private void putRequiredParameter(Map<String, String> parameters, String key, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("훈련기관 정보 요청 파라미터 " + key + "을 찾을 수 없습니다.");
		}
		parameters.put(key, value);
	}

	private void putOptionalParameter(Map<String, String> parameters, String key, String value) {
		if (value != null && !value.isBlank()) {
			parameters.put(key, value);
		}
	}

	private String firstNonBlank(String first, String second) {
		return first == null || first.isBlank() ? second : first;
	}

	private Map<String, String> queryParameters(String sourceUrl) {
		Map<String, String> parameters = new LinkedHashMap<>();
		String rawQuery = URI.create(sourceUrl).getRawQuery();
		if (rawQuery == null || rawQuery.isBlank()) {
			return parameters;
		}

		for (String pair : rawQuery.split("&")) {
			int separatorIndex = pair.indexOf('=');
			String key = separatorIndex < 0 ? pair : pair.substring(0, separatorIndex);
			String value = separatorIndex < 0 ? "" : pair.substring(separatorIndex + 1);
			parameters.put(urlDecode(key), urlDecode(value));
		}
		return parameters;
	}

	private String urlDecode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private Path resolveOutputPath(Work24TrainingCourseOverviewCrawlRequest request) {
		String outputPath = request == null || request.outputPath() == null || request.outputPath().isBlank()
			? properties.outputPath()
			: request.outputPath();
		return Path.of(outputPath);
	}
}

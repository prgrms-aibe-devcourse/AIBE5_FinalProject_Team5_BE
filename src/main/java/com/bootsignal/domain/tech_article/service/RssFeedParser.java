package com.bootsignal.domain.tech_article.service;

import com.bootsignal.domain.tech_article.config.TechArticleRssProperties;
import com.bootsignal.domain.tech_article.dto.ParsedRssArticle;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/* RSS 피드 파서 */
@Service
@RequiredArgsConstructor
public class RssFeedParser {

	private static final DateTimeFormatter RSS_PUB_DATE = DateTimeFormatter.RFC_1123_DATE_TIME;
	private static final Pattern IMG_SRC_PATTERN = Pattern.compile(
		"<img[^>]+src=[\"']([^\"']+)[\"']",
		Pattern.CASE_INSENSITIVE
	);

	private final TechArticleRssProperties properties;

	// RSS 피드 가져오기 및 파싱
	public List<ParsedRssArticle> fetchAndParse(ArticleSource source) throws IOException {
		// RSS 피드 가져오기
		Document document = Jsoup.connect(properties.feedUrlOf(source))
			.userAgent(properties.userAgent())
			.timeout(properties.timeoutMillis())
			.parser(Parser.xmlParser())
			.get();

		return parse(document, properties.collectLimit());
	}

	// RSS 피드 파싱
	public List<ParsedRssArticle> parse(Document document, int limit) {
		Elements items = document.select("item");
		LocalDateTime channelFallbackPublishedAt = parseChannelLastBuildDate(document);

		return items.stream()
			.map(item -> parseItem(item, channelFallbackPublishedAt))
			.sorted(Comparator.comparing(ParsedRssArticle::publishedAt).reversed())
			.limit(limit)
			.toList();
	}

	// RSS 피드 아이템 파싱
	private ParsedRssArticle parseItem(Element item, LocalDateTime channelFallbackPublishedAt) {
		String title = textOf(item, "title");
		String articleUrl = textOf(item, "link");
		String rawDescription = textOf(item, "description");
		String summary = toPlainSummary(rawDescription);
		String rssGuid = firstNonBlank(textOf(item, "guid"), articleUrl);
		String author = firstNonBlank(textOf(item, "author"), textOf(item, "dc|creator"));
		String contentEncoded = textOf(item, "content|encoded");
		String thumbnailUrl = extractThumbnailUrl(item, contentEncoded, rawDescription);
		LocalDateTime publishedAt = firstNonNull(
			parsePublishedAt(textOf(item, "pubDate")),
			channelFallbackPublishedAt,
			LocalDateTime.now()
		);

		return ParsedRssArticle.of(
			title,
			summary,
			thumbnailUrl,
			author,
			articleUrl,
			publishedAt,
			rssGuid
		);
	}

	// 썸네일 URL 추출
	private String extractThumbnailUrl(Element item, String contentEncoded, String description) {
		String fromThumbnailTag = textOf(item, "thumbnail");
		if (fromThumbnailTag != null && !fromThumbnailTag.isBlank()) {
			return fromThumbnailTag.trim();
		}
		String fromContent = extractFirstImageSrc(contentEncoded);
		if (fromContent != null) {
			return fromContent;
		}
		return extractFirstImageSrc(description);
	}

	// 설명 텍스트 요약 텍스트 변환
	private String toPlainSummary(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		String plainText = Jsoup.parse(description).text().trim();
		return plainText.isEmpty() ? null : plainText;
	}

	// 채널 마지막 빌드 날짜 파싱 (게시 날짜 추출용)
	private LocalDateTime parseChannelLastBuildDate(Document document) {
		return parsePublishedAt(textOf(document.selectFirst("channel"), "lastBuildDate"));
	}

	// 게시 날짜 파싱
	private LocalDateTime parsePublishedAt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return ZonedDateTime.parse(value.trim(), RSS_PUB_DATE.withLocale(Locale.ENGLISH))
				.toLocalDateTime();
		} catch (DateTimeParseException ignored) {
			return null;
		}
	}

	// 첫 번째 이미지 (썸네일) URL 추출
	private String extractFirstImageSrc(String html) {
		if (html == null || html.isBlank()) {
			return null;
		}
		Matcher matcher = IMG_SRC_PATTERN.matcher(html);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group(1);
	}

	private String textOf(Element parent, String cssQuery) {
		if (parent == null) {
			return null;
		}
		Element element = parent.selectFirst(cssQuery);
		return element != null ? element.text() : null;
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first.trim();
		}
		if (second != null && !second.isBlank()) {
			return second.trim();
		}
		return null;
	}

	@SafeVarargs
	private <T> T firstNonNull(T... values) {
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}
}

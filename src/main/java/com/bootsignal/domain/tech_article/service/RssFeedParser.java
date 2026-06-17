package com.bootsignal.domain.tech_article.service;

import com.bootsignal.domain.tech_article.config.TechArticleRssProperties;
import com.bootsignal.domain.tech_article.dto.ParsedRssArticle;
import com.bootsignal.domain.tech_article.entity.ArticleSource;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/* RSS 피드 파서 */
@Service
@RequiredArgsConstructor
public class RssFeedParser {

	private static final int SUMMARY_MAX_LENGTH = 100;
	private static final DateTimeFormatter RSS_PUB_DATE = DateTimeFormatter.RFC_1123_DATE_TIME; // RFC 1123 형식 날짜 파싱용
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

		return parseAll(document);
	}

	// RSS/Atom 피드 전체 항목 파싱 (발행일 최신순)
	public List<ParsedRssArticle> parseAll(Document document) {
		Elements items = document.select("item");
		if (!items.isEmpty()) {
			LocalDateTime channelFallbackPublishedAt = parsePublishedAt(
				textOf(document.selectFirst("channel"), "lastBuildDate"));
			return items.stream()
				.map(item -> parseRssItem(item, channelFallbackPublishedAt))
				.sorted(Comparator.comparing(ParsedRssArticle::publishedAt).reversed())
				.toList();
		}

		Elements entries = document.select("entry");
		LocalDateTime feedFallbackPublishedAt = parsePublishedAt(
			textOf(document.selectFirst("feed"), "updated"));
		return entries.stream()
			.map(entry -> parseAtomEntry(entry, feedFallbackPublishedAt))
			.sorted(Comparator.comparing(ParsedRssArticle::publishedAt).reversed())
			.toList();
	}

	// RSS 피드 파싱 (limit 적용)
	public List<ParsedRssArticle> parse(Document document, int limit) {
		return parseAll(document).stream()
			.limit(limit)
			.toList();
	}

	// Atom entry 파싱
	private ParsedRssArticle parseAtomEntry(Element entry, LocalDateTime feedFallbackPublishedAt) {
		String title = normalizeText(textOf(entry, "title"));
		String articleUrl = linkHrefOf(entry);
		String rawContent = firstNonBlank(textOf(entry, "content"), textOf(entry, "summary"));
		String summary = toPlainSummary(rawContent);
		String rssGuid = firstNonBlank(textOf(entry, "id"), articleUrl);
		String author = textOf(entry.selectFirst("author"), "name");
		String thumbnailUrl = extractFirstImageSrc(rawContent);
		LocalDateTime publishedAt = firstNonNull(
			parsePublishedAt(textOf(entry, "published")),
			parsePublishedAt(textOf(entry, "updated")),
			feedFallbackPublishedAt,
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

	// RSS item 파싱
	private ParsedRssArticle parseRssItem(Element item, LocalDateTime channelFallbackPublishedAt) {
		String title = normalizeText(textOf(item, "title"));
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

	// 설명 텍스트를 plain summary로 변환 (미리보기용 최대 100자)
	private String toPlainSummary(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		String plainText = Jsoup.parse(description).text().trim();
		if (plainText.isEmpty()) {
			return null;
		}
		if (plainText.length() <= SUMMARY_MAX_LENGTH) {
			return plainText;
		}
		return plainText.substring(0, SUMMARY_MAX_LENGTH) + "…";
	}

	// 텍스트 정규화
	private String normalizeText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Entities.unescape(value).trim();
	}

	// 게시 날짜 파싱 (RFC 1123, ISO-8601)
	private LocalDateTime parsePublishedAt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		try {
			return ZonedDateTime.parse(trimmed, RSS_PUB_DATE.withLocale(Locale.ENGLISH))
				.toLocalDateTime();
		} catch (DateTimeParseException ignored) {
			// RFC 1123 형식이 아니면 ISO-8601(Atom) 시도
		}
		try {
			return Instant.parse(trimmed).atZone(ZoneOffset.UTC).toLocalDateTime();
		} catch (DateTimeParseException ignored) {
			return null;
		}
	}

	// 링크 URL 추출
	private String linkHrefOf(Element parent) {
		if (parent == null) {
			return null;
		}
		Element alternateLink = parent.selectFirst("link[rel=alternate][href]");
		if (alternateLink != null) {
			return alternateLink.attr("href");
		}
		Element link = parent.selectFirst("link[href]");
		return link != null ? link.attr("href") : null;
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

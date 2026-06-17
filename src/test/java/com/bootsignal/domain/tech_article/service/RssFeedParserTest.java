package com.bootsignal.domain.tech_article.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.tech_article.config.TechArticleRssProperties;
import com.bootsignal.domain.tech_article.dto.ParsedRssArticle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;

@DisplayName("RssFeedParser 테스트")
class RssFeedParserTest {

	private static final String RSS_SAMPLE_BASE = "rss-sample/";

	private RssFeedParser parser;

	@BeforeEach
	void setUp() {
		parser = new RssFeedParser(testProperties());
	}

	private static TechArticleRssProperties testProperties() {
		return new TechArticleRssProperties(
			"https://yozm.wishket.com/magazine/feed/",
			"https://tech.kakao.com/feed/",
			"https://d2.naver.com/d2.atom",
			"https://techblog.woowahan.com/feed/",
			"https://toss.tech/rss.xml",
			30,
			6,
			15_000,
			"BootSignalTest/1.0"
		);
	}

	@Test
	@DisplayName("요즘IT 샘플 RSS를 파싱하면 제목·URL·썸네일·발행일이 추출된다")
	void parseYozmSampleFixture() throws IOException {
		String xml = new ClassPathResource(RSS_SAMPLE_BASE + "yozm-feed-sample.xml")
			.getContentAsString(StandardCharsets.UTF_8);
		var document = Jsoup.parse(xml, "", Parser.xmlParser());

		var articles = parser.parse(document, 30);

		assertThat(articles).hasSize(2);

		ParsedRssArticle first = articles.getFirst();
		assertThat(first.title()).contains("페이블 5");
		assertThat(first.articleUrl()).isEqualTo("https://yozm.wishket.com/magazine/detail/3805");
		assertThat(first.rssGuid()).isEqualTo("https://yozm.wishket.com/magazine/detail/3805");
		assertThat(first.summary()).contains("출시 사흘 만에");
		assertThat(first.thumbnailUrl()).contains("sample.jpg");
		assertThat(first.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 16, 9, 0));
	}

	@Test
	@DisplayName("카카오 테크 샘플 RSS를 파싱하면 작성자·thumbnail 태그가 추출된다")
	void parseKakaoSampleFixture() throws IOException {
		String xml = new ClassPathResource(RSS_SAMPLE_BASE + "kakao-feed-sample.xml")
			.getContentAsString(StandardCharsets.UTF_8);
		var document = Jsoup.parse(xml, "", Parser.xmlParser());

		var articles = parser.parse(document, 30);

		assertThat(articles).hasSize(1);
		ParsedRssArticle article = articles.getFirst();
		assertThat(article.title()).contains("MCP Player 10");
		assertThat(article.articleUrl()).isEqualTo("https://tech.kakao.com/posts/818");
		assertThat(article.author()).isEqualTo("kakao.AI");
		assertThat(article.thumbnailUrl()).contains("sample.jpg");
		assertThat(article.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 8, 15, 0));
	}

	@Test
	@DisplayName("D2 Atom 샘플 피드를 파싱하면 제목·URL·발행일이 추출된다")
	void parseD2AtomSampleFixture() throws IOException {
		String xml = new ClassPathResource(RSS_SAMPLE_BASE + "d2-atom-feed-sample.xml")
			.getContentAsString(StandardCharsets.UTF_8);
		var document = Jsoup.parse(xml, "", Parser.xmlParser());

		var articles = parser.parse(document, 30);

		assertThat(articles).hasSize(2);
		ParsedRssArticle first = articles.getFirst();
		assertThat(first.title()).contains("SaaS 대체하기");
		assertThat(first.articleUrl()).isEqualTo("https://d2.naver.com/helloworld/8319114");
		assertThat(first.rssGuid()).isEqualTo("https://d2.naver.com/helloworld/8319114");
		assertThat(first.summary()).contains("에러 모니터링");
		assertThat(first.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 16, 14, 14, 39));
	}

	@Test
	@DisplayName("발행일 최신순 정렬 후 limit만큼만 반환한다")
	void parseAppliesLimitAfterSortingByPublishedAt() throws IOException {
		String xml = new ClassPathResource(RSS_SAMPLE_BASE + "yozm-feed-sample.xml")
			.getContentAsString(StandardCharsets.UTF_8);
		var document = Jsoup.parse(xml, "", Parser.xmlParser());

		var articles = parser.parse(document, 1);

		assertThat(articles).hasSize(1);
		assertThat(articles.getFirst().title()).contains("페이블 5");
	}

	@Test
	@DisplayName("summary는 100자를 초과하면 잘라 저장한다")
	void truncateSummaryToMaxLength() {
		String longText = "가".repeat(120);
		String xml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<rss><channel>
			  <item>
			    <title>긴 본문 미리보기</title>
			    <link>https://example.com/1</link>
			    <guid>https://example.com/1</guid>
			    <description><![CDATA[%s]]></description>
			    <pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>
			  </item>
			</channel></rss>
			""".formatted(longText);
		var document = Jsoup.parse(xml, "", Parser.xmlParser());

		var articles = parser.parse(document, 1);

		assertThat(articles.getFirst().summary()).hasSize(101);
		assertThat(articles.getFirst().summary()).endsWith("…");
		assertThat(articles.getFirst().summary()).startsWith("가".repeat(100));
	}

	@Test
	@EnabledIfSystemProperty(named = "rss.live.test", matches = "true")
	@DisplayName("실제 RSS/Atom 피드를 fetch하면 소스별 항목이 파싱된다")
	void fetchLiveFeeds() throws IOException {
		for (var source : com.bootsignal.domain.tech_article.entity.ArticleSource.values()) {
			var articles = parser.fetchAndParse(source);
			assertThat(articles).isNotEmpty();
			assertThat(articles.getFirst().title()).isNotBlank();
			assertThat(articles.getFirst().articleUrl()).isNotBlank();
			assertThat(articles.getFirst().rssGuid()).isNotBlank();
		}
	}
}

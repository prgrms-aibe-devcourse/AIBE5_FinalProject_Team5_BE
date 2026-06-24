package com.bootsignal.domain.work24.service;

import com.bootsignal.domain.work24.config.Work24CrawlerProperties;
import com.bootsignal.domain.work24.dto.ReviewCrawlResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 고용24 과정 만족도 페이지(/hr/a/a/3300/selectStdgEmrt5.do)에서
 * 수강후기를 크롤링하는 서비스.
 *
 * NOTE: 실제 HTML selector는 페이지 접근 후 검증 필요.
 * 현재 구현은 고용24 공통 패턴을 기반으로 작성됨.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewPageCrawlerService {

    private static final String REVIEW_PAGE_PATH = "/hr/a/a/3300/selectStdgEmrt5.do";
    private static final Pattern INTEGER_PATTERN = Pattern.compile("(\\d+)");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final Work24CrawlerProperties properties;

    /**
     * titleLink의 path만 수강후기 경로로 교체하여 URL을 조립한다.
     * titleLink 쿼리스트링을 그대로 재사용하고 mainTracseSe, prtcmpSeqNo만 추가한다.
     */
    public String resolveReviewPageUrl(String titleLink) {
        log.info("[DEBUG] titleLink 원본: '{}'", titleLink);
        int pathIdx = titleLink.indexOf("/hr/");
        if (pathIdx < 0) {
            log.warn("수강후기 URL 조립 실패 - /hr/ 경로 없음: {}", titleLink);
            return null;
        }

        String origin = titleLink.substring(0, pathIdx);   // https://www.work24.go.kr

        int queryIdx = titleLink.indexOf('?');
        String existingQuery = queryIdx >= 0 ? titleLink.substring(queryIdx + 1) : "";

        Map<String, String> params = queryParameters(titleLink);
        String crseTracseSe = params.get("crseTracseSe");
        if (crseTracseSe == null) {
            log.warn("수강후기 URL 조립 실패 - crseTracseSe 없음: {}", titleLink);
            return null;
        }
        String mainTracseSe = params.getOrDefault("mainTracseSe", crseTracseSe);

        return origin + REVIEW_PAGE_PATH + "?" + existingQuery
                + "&mainTracseSe=" + mainTracseSe + "&prtcmpSeqNo=";
    }

    /**
     * 수강후기 페이지를 fetch하여 페이지네이션을 순회하며 리뷰를 수집한다.
     * maxPages 상한으로 과도한 크롤링을 방지한다.
     *
     * @param titleLink   CourseSession.titleLink
     * @param delayMillis 페이지 간 요청 딜레이 (ms)
     * @param maxPages    최대 크롤링 페이지 수 (페이지당 10건, 기본 10 = 최대 100건)
     */
    public List<ReviewCrawlResult> fetchAndParseAllReviews(String titleLink, long delayMillis, int maxPages)
            throws IOException, InterruptedException {

        log.info("[DEBUG] fetchAndParseAllReviews - titleLink: '{}', maxPages: {}", titleLink, maxPages);
        int pathIdx = titleLink.indexOf("/hr/");
        if (pathIdx < 0) {
            log.warn("수강후기 크롤링 실패 - /hr/ 경로 없음: {}", titleLink);
            return List.of();
        }

        String origin = titleLink.substring(0, pathIdx);   // https://www.work24.go.kr
        String ajaxUrl = origin + "/hr/a/a/3300/selectTgcrAtlcRvw.do";

        Map<String, String> params = queryParameters(titleLink);
        String tracseId = params.get("tracseId");
        String tracseTme = params.get("tracseTme");
        String crseTracseSe = params.get("crseTracseSe");
        String trainstCstmrId = params.get("trainstCstmrId");

        if (tracseId == null || tracseTme == null || crseTracseSe == null || trainstCstmrId == null) {
            log.warn("수강후기 크롤링 실패 - 필수 파라미터 누락: {}", titleLink);
            return List.of();
        }

        String mainTracseSe = params.getOrDefault("mainTracseSe", crseTracseSe);

        List<ReviewCrawlResult> all = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            // 최대 페이지 수 초과 시 조기 종료 (과도한 크롤링 및 DB 적재 방지)
            if (pageNo > maxPages) {
                log.info("수강후기 최대 페이지({}) 도달 — 조기 종료 (titleLink={})", maxPages, titleLink);
                break;
            }

            log.debug("수강후기 크롤링 AJAX 요청 (pageNo={}/{}, url={})", pageNo, maxPages, ajaxUrl);

            Map<String, String> postData = new LinkedHashMap<>();
            postData.put("tracseId", tracseId);
            postData.put("tracseTme", tracseTme);
            postData.put("crseTracseSe", crseTracseSe);
            postData.put("trainstCstmrId", trainstCstmrId);
            postData.put("mainTracseSe", mainTracseSe);
            postData.put("tgcrSe", "01");
            postData.put("pageIndex", String.valueOf(pageNo));
            postData.put("pageSize", "10");

            Document doc = Jsoup.connect(ajaxUrl)
                    .userAgent(properties.userAgent())
                    .timeout(properties.timeoutMillis())
                    .referrer(titleLink)
                    .data(postData)
                    .post();

            List<ReviewCrawlResult> pageResults = parseReviews(doc, pageNo);
            if (pageResults.isEmpty()) {
                break;
            }

            all.addAll(pageResults);
            pageNo++;

            if (!hasNextPage(doc, pageNo)) {
                break;
            }

            Thread.sleep(delayMillis);
        }

        log.info("수강후기 수집 완료 (총 {}건, titleLink={})", all.size(), titleLink);
        return all;
    }

    /**
     * 기본 maxPages(10)를 사용하는 편의 메서드. 하위 호환성 유지용.
     */
    public List<ReviewCrawlResult> fetchAndParseAllReviews(String titleLink, long delayMillis)
            throws IOException, InterruptedException {
        return fetchAndParseAllReviews(titleLink, delayMillis, 10);
    }

    // ───────────────────────────────────────────────────
    // HTML 파싱 (실제 selector는 크롤링 테스트 후 보정 필요)
    // ───────────────────────────────────────────────────

    /**
     * 수강후기 목록 HTML을 파싱하여 ReviewCrawlResult 목록을 반환한다.
     * selector는 고용24 공통 패턴 기반이며, 실제 페이지 확인 후 조정 필요.
     */
    List<ReviewCrawlResult> parseReviews(Document doc, int pageNo) {
        // 수강후기 항목 선택 — 실제 selector는 페이지 확인 후 조정
        Elements items = doc.select(".hr_review_list_item, .cmts_list li, .review_list li, .board_list tbody tr");
        if (items.isEmpty()) {
            log.debug("수강후기 항목 없음 (pageNo={})", pageNo);
            return List.of();
        }

        List<ReviewCrawlResult> results = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Element item = items.get(i);
            String externalReviewId = pageNo + "_" + (i + 1);

            // data-id 또는 id 속성이 있으면 우선 사용
            String dataId = item.attr("data-id");
            if (!dataId.isBlank()) {
                externalReviewId = dataId;
            }

            String nickname = extractNickname(item);
            Integer rating = extractRating(item);
            String content = extractContent(item);
            LocalDateTime reviewedAt = extractDate(item);

            if (content == null || content.isBlank()) {
                continue;
            }

            results.add(new ReviewCrawlResult(externalReviewId, nickname, rating, content, reviewedAt));
        }
        return results;
    }

    private String extractNickname(Element item) {
        Element el = item.selectFirst(".writer, .name, .user_name, .nick, .id, td.writer");
        return el != null ? el.text().trim() : null;
    }

    private Integer extractRating(Element item) {
        // 별점: 공통 패턴은 .star_on 개수 또는 aria-label, data-score, 또는 .s1_r의 텍스트
        Element ratingEl = item.selectFirst(".s1_r, [data-score], .score, .star .on, .rating");
        if (ratingEl != null) {
            String text = ratingEl.text().trim();
            if (!text.isBlank()) {
                Matcher m = INTEGER_PATTERN.matcher(text);
                if (m.find()) return Integer.parseInt(m.group(1));
            }
            String score = ratingEl.attr("data-score");
            if (!score.isBlank()) {
                Matcher m = INTEGER_PATTERN.matcher(score);
                if (m.find()) return Integer.parseInt(m.group(1));
            }
            int starCount = item.select(".star_on, .on").size();
            if (starCount > 0) return Math.min(starCount, 5);
        }
        return null;
    }

    private String extractContent(Element item) {
        Element el = item.selectFirst(".content, .cmts_content, .text, .desc, td.content, .review_text");
        return el != null ? el.text().trim() : null;
    }

    private LocalDateTime extractDate(Element item) {
        Element el = item.selectFirst(".date, .reg_dt, .write_date, .wdate, td.date");
        if (el == null) return null;
        String text = el.text().trim();
        try {
            if (text.contains(":")) return LocalDateTime.parse(text, DATETIME_FORMATTER);
            return java.time.LocalDate.parse(text, DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean hasNextPage(Document doc, int nextPageNo) {
        // 페이지네이션 링크에 다음 페이지 번호가 있는지 확인
        Elements pageLinks = doc.select(".box_pagination button, .paging a, .pagination a, .page_num a");
        return pageLinks.stream()
                .anyMatch(el -> el.text().trim().equals(String.valueOf(nextPageNo)));
    }

    // ───────────────────────────────────────────────────
    // URL 유틸
    // ───────────────────────────────────────────────────

    private Map<String, String> queryParameters(String url) {
        Map<String, String> parameters = new LinkedHashMap<>();
        int queryStart = url.indexOf('?');
        if (queryStart < 0) return parameters;
        String rawQuery = url.substring(queryStart + 1);
        if (rawQuery.isBlank()) return parameters;

        for (String pair : rawQuery.split("&")) {
            int sep = pair.indexOf('=');
            String key = sep < 0 ? pair : pair.substring(0, sep);
            String value = sep < 0 ? "" : pair.substring(sep + 1);
            parameters.put(urlDecode(key), urlDecode(value));
        }
        return parameters;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}

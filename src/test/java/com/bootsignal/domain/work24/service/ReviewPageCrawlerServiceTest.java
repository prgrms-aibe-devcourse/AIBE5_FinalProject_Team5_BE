package com.bootsignal.domain.work24.service;

import com.bootsignal.domain.work24.config.Work24CrawlerProperties;
import com.bootsignal.domain.work24.dto.ReviewCrawlResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("고용24 실서버 연동 검증용 테스트 CI 환경 실행 제외")
class ReviewPageCrawlerServiceTest {

    private final ReviewPageCrawlerService service = new ReviewPageCrawlerService(
            new Work24CrawlerProperties(
                    "https://www.work24.go.kr/hr/a/a/3100/selectTracseDetl.do?tracseId=AIG20240000498288&tracseTme=7&crseTracseSe=C0061&trainstCstmrId=500036172479",
                    "build/crawled/test.json",
                    10000,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
    );

    @Test
    @DisplayName("fetchAndParseAllReviews 통합 테스트 (실제 고용24 연동)")
    void testFetchAndParseAllReviews() throws IOException, InterruptedException {
        String titleLink = "https://www.work24.go.kr/hr/a/a/3100/selectTracseDetl.do?tracseId=AIG20240000498288&tracseTme=7&crseTracseSe=C0061&trainstCstmrId=500036172479";
        
        List<ReviewCrawlResult> reviews = service.fetchAndParseAllReviews(titleLink, 500);
        
        System.out.println("=== 크롤링한 총 리뷰 개수: " + reviews.size() + " ===");
        assertThat(reviews).isNotEmpty();
        
        // 상위 5개 리뷰 정보 출력
        reviews.stream().limit(5).forEach(r -> {
            System.out.println("ID: " + r.externalReviewId() + 
                               ", 별점: " + r.rating() + 
                               ", 내용: " + r.content());
            assertThat(r.rating()).isBetween(1, 5);
            assertThat(r.content()).isNotBlank();
        });
    }
}

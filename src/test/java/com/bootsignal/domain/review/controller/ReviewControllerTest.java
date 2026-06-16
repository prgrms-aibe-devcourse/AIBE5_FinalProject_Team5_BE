package com.bootsignal.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.review.dto.ReviewCreateRequest;
import com.bootsignal.domain.review.dto.ReviewResponse;
import com.bootsignal.domain.review.dto.ReviewStatisticsResponse;
import com.bootsignal.domain.review.dto.ReviewUpdateRequest;
import com.bootsignal.domain.review.dto.VerifiedReviewDetailResponse;
import com.bootsignal.domain.review.entity.ReviewType;
import com.bootsignal.domain.review.service.ReviewService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 리뷰 컨트롤러가 프론트 요청 값을 DTO로 매핑하고 공통 응답 형식으로 반환하는지 검증합니다.
 */
@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReviewController 테스트")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @DisplayName("POST /api/courses/{courseId}/reviews - 일반 리뷰 작성 요청의 overallRating alias를 매핑한다")
    void createMapsOverallRatingAlias() throws Exception {
        given(reviewService.create(eq(10L), any(ReviewCreateRequest.class)))
            .willReturn(reviewResponse(100L, ReviewType.GENERAL, 4, "좋은 과정입니다.", null));

        mockMvc.perform(post("/api/courses/{courseId}/reviews", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseSessionId": 20,
                      "reviewType": "GENERAL",
                      "overallRating": 4,
                      "content": "좋은 과정입니다."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reviewId").value(100))
            .andExpect(jsonPath("$.data.reviewType").value("GENERAL"))
            .andExpect(jsonPath("$.data.rating").value(4))
            .andExpect(jsonPath("$.data.verifiedDetail").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<ReviewCreateRequest> captor = ArgumentCaptor.forClass(ReviewCreateRequest.class);
        verify(reviewService).create(eq(10L), captor.capture());
        assertThat(captor.getValue().rating()).isEqualTo(4);
        assertThat(captor.getValue().content()).isEqualTo("좋은 과정입니다.");
    }

    @Test
    @DisplayName("PATCH /api/reviews/{reviewId} - 인증 리뷰 상세 설문 수정 요청을 매핑한다")
    void updateMapsVerifiedDetailRequest() throws Exception {
        given(reviewService.update(eq(100L), any(ReviewUpdateRequest.class)))
            .willReturn(reviewResponse(100L, ReviewType.VERIFIED, 2, "수정된 인증 리뷰", verifiedDetailResponse(2)));

        mockMvc.perform(patch("/api/reviews/{reviewId}", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifiedUpdateJson(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reviewType").value("VERIFIED"))
            .andExpect(jsonPath("$.data.rating").value(2))
            .andExpect(jsonPath("$.data.verifiedDetail.instructorDeliveryRating").value(2))
            .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<ReviewUpdateRequest> captor = ArgumentCaptor.forClass(ReviewUpdateRequest.class);
        verify(reviewService).update(eq(100L), captor.capture());
        assertThat(captor.getValue().rating()).isNull();
        assertThat(captor.getValue().verifiedDetail().age()).isEqualTo(29);
        assertThat(captor.getValue().verifiedDetail().instructorDeliveryRating()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/courses/{courseId}/reviews/statistics - 인증 리뷰 통계를 반환한다")
    void getStatisticsReturnsReviewStatistics() throws Exception {
        ReviewStatisticsResponse response = new ReviewStatisticsResponse(
            1,
            BigDecimal.valueOf(4.5),
            List.of(new ReviewStatisticsResponse.RatingBarItem(5, 1)),
            List.of(new ReviewStatisticsResponse.PriorKnowledgeDistributionItem("non_major", "비전공", 1, "#5C6AC4")),
            List.of(new ReviewStatisticsResponse.QualityMetricItem("강사 전달력", BigDecimal.valueOf(4.5)))
        );
        given(reviewService.getStatistics(10L)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/reviews/statistics", 10L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reviewCount").value(1))
            .andExpect(jsonPath("$.data.averageRating").value(4.5))
            .andExpect(jsonPath("$.data.ratingBars[0].score").value(5))
            .andExpect(jsonPath("$.data.priorKnowledgeDistribution[0].level").value("비전공"))
            .andExpect(jsonPath("$.data.qualityMetrics[0].label").value("강사 전달력"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    private ReviewResponse reviewResponse(
        Long reviewId,
        ReviewType reviewType,
        Integer rating,
        String content,
        VerifiedReviewDetailResponse verifiedDetail
    ) {
        return new ReviewResponse(
            reviewId,
            1L,
            "tester",
            10L,
            20L,
            reviewType,
            rating,
            content,
            verifiedDetail,
            LocalDateTime.of(2026, 6, 16, 10, 0),
            LocalDateTime.of(2026, 6, 16, 10, 0)
        );
    }

    private VerifiedReviewDetailResponse verifiedDetailResponse(Integer score) {
        return new VerifiedReviewDetailResponse(
            "비전공",
            29,
            "취업",
            "온라인",
            1,
            "중",
            "적당",
            "중",
            3,
            score,
            score,
            score,
            3,
            score,
            score,
            score,
            "수료",
            null,
            null,
            "준비중",
            "수정된 인증 리뷰"
        );
    }

    private String verifiedUpdateJson(Integer score) {
        return """
            {
              "content": "수정된 인증 리뷰",
              "verifiedDetail": {
                "priorKnowledgeLevel": "non_major",
                "age": "29",
                "learningGoal": "employment",
                "attendanceType": "online",
                "cohort": "1",
                "courseDifficulty": "medium",
                "progressSpeed": "moderate",
                "teamProjectDifficulty": "medium",
                "avgSelfStudyHours": "3",
                "instructorDeliveryRating": %d,
                "curriculumRating": %d,
                "employmentSupportRating": %d,
                "projectCount": "3",
                "projectAchievementRating": %d,
                "toolSupportRating": %d,
                "mentoringSatisfactionRating": %d,
                "completionStatus": "completed",
                "employmentStatus": "preparing",
                "collaborationComment": "수정된 인증 리뷰"
              }
            }
            """.formatted(score, score, score, score, score, score);
    }
}

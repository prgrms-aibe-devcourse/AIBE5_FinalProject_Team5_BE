package com.bootsignal.domain.review.dto;

import com.bootsignal.domain.review.entity.Review;
import com.bootsignal.domain.review.entity.ReviewPriorKnowledgeLevel;
import com.bootsignal.domain.review.entity.ReviewVerifiedDetail;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 과정 상세와 과정 비교 화면에서 사용하는 인증 리뷰 통계 응답 DTO입니다.
 */
public record ReviewStatisticsResponse(
    long reviewCount,
    BigDecimal averageRating,
    List<RatingBarItem> ratingBars,
    List<PriorKnowledgeDistributionItem> priorKnowledgeDistribution,
    List<QualityMetricItem> qualityMetrics
) {
    public static ReviewStatisticsResponse from(List<Review> reviews) {
        List<Review> validReviews = reviews.stream()
            .filter(review -> review.getVerifiedDetail() != null)
            .toList();

        long reviewCount = validReviews.size();
        List<RatingBarItem> ratingBars = List.of(5, 4, 3, 2, 1).stream()
            .map(score -> new RatingBarItem(
                score,
                validReviews.stream().filter(review -> review.getRating() == score).count()
            ))
            .toList();

        List<PriorKnowledgeDistributionItem> priorKnowledgeDistribution = Arrays.stream(ReviewPriorKnowledgeLevel.values())
            .map(level -> new PriorKnowledgeDistributionItem(
                level.value(),
                level.label(),
                validReviews.stream()
                    .filter(review -> review.getVerifiedDetail().getPriorKnowledgeLevel() == level)
                    .count(),
                level.color()
            ))
            .toList();

        List<ReviewVerifiedDetail> details = validReviews.stream()
            .map(Review::getVerifiedDetail)
            .toList();

        return new ReviewStatisticsResponse(
            reviewCount,
            average(validReviews.stream().mapToInt(Review::getRating).sum(), reviewCount),
            ratingBars,
            priorKnowledgeDistribution,
            List.of(
                qualityMetric("강사 전달력", details, ReviewVerifiedDetail::getInstructorDeliveryRating),
                qualityMetric("커리큘럼", details, ReviewVerifiedDetail::getCurriculumRating),
                qualityMetric("취업 지원", details, ReviewVerifiedDetail::getEmploymentSupportSatisfactionRating),
                qualityMetric("프로젝트 성취도", details, ReviewVerifiedDetail::getProjectAchievementRating),
                qualityMetric("툴 지원", details, ReviewVerifiedDetail::getToolSupportRating),
                qualityMetric("멘토링", details, ReviewVerifiedDetail::getMentoringSatisfactionRating)
            )
        );
    }

    private static QualityMetricItem qualityMetric(
        String label,
        List<ReviewVerifiedDetail> details,
        ToIntFunction<ReviewVerifiedDetail> valueExtractor
    ) {
        long count = details.size();
        int sum = details.stream().mapToInt(valueExtractor).sum();
        return new QualityMetricItem(label, average(sum, count));
    }

    private static BigDecimal average(int sum, long count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    public record RatingBarItem(
        int score,
        long count
    ) {
    }

    public record PriorKnowledgeDistributionItem(
        String value,
        String level,
        long count,
        String color
    ) {
    }

    public record QualityMetricItem(
        String label,
        BigDecimal value
    ) {
    }
}

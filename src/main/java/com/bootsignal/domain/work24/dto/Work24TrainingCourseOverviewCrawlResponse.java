package com.bootsignal.domain.work24.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record Work24TrainingCourseOverviewCrawlResponse(
	String sourceUrl,
	String savedPath,
	String trainingTargetRequirements,
	String trainingGoal,
	Integer confirmedTraineeCount,
	Integer selectedTraineeCount,
	Integer recruitmentCount,
	BigDecimal employmentRate,
	String institutionProfileImageUrl,
	String institutionIntroduction,
	Instant crawledAt
) {

	public static Work24TrainingCourseOverviewCrawlResponse from(Work24TrainingCourseOverviewSaveResult result) {
		Work24TrainingCourseOverview overview = result.overview();
		return new Work24TrainingCourseOverviewCrawlResponse(
			overview.sourceUrl(),
			result.savedPath(),
			overview.trainingTargetRequirements(),
			overview.trainingGoal(),
			overview.confirmedTraineeCount(),
			overview.selectedTraineeCount(),
			overview.recruitmentCount(),
			overview.employmentRate(),
			overview.institutionProfileImageUrl(),
			overview.institutionIntroduction(),
			overview.crawledAt()
		);
	}
}

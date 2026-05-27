package com.bootsignal.domain.work24.dto;

import java.time.Instant;

public record Work24TrainingCourseOverviewCrawlResponse(
	String sourceUrl,
	String savedPath,
	String trainingTargetRequirements,
	String trainingGoal,
	Instant crawledAt
) {

	public static Work24TrainingCourseOverviewCrawlResponse from(Work24TrainingCourseOverviewSaveResult result) {
		Work24TrainingCourseOverview overview = result.overview();
		return new Work24TrainingCourseOverviewCrawlResponse(
			overview.sourceUrl(),
			result.savedPath(),
			overview.trainingTargetRequirements(),
			overview.trainingGoal(),
			overview.crawledAt()
		);
	}
}

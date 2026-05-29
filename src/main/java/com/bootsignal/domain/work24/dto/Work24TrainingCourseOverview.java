package com.bootsignal.domain.work24.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record Work24TrainingCourseOverview(
	String sourceUrl,
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
}

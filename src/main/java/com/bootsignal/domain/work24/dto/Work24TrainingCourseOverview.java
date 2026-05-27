package com.bootsignal.domain.work24.dto;

import java.time.Instant;

public record Work24TrainingCourseOverview(
	String sourceUrl,
	String trainingTargetRequirements,
	String trainingGoal,
	Instant crawledAt
) {
}

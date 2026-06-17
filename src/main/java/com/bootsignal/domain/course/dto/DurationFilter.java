package com.bootsignal.domain.course.dto;

/**
 * 훈련 기간 필터 (CourseSession.totalTrainingDays 기준)
 * - WITHIN_3_MONTHS : 총 훈련일수 90일 이하
 * - WITHIN_6_MONTHS : 총 훈련일수 91일 이상 180일 이하
 * - OVER_6_MONTHS   : 총 훈련일수 181일 이상
 */
public enum DurationFilter {
    WITHIN_3_MONTHS,
    WITHIN_6_MONTHS,
    OVER_6_MONTHS
}

package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 난이도 수준을 표현하는 enum입니다.
 */
public enum ReviewDifficultyLevel implements ReviewSurveyOption {
    HIGH("high", "상"),
    MEDIUM("medium", "중"),
    LOW("low", "하");

    private final String value;
    private final String label;

    ReviewDifficultyLevel(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    @JsonCreator
    public static ReviewDifficultyLevel from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 난이도 수준입니다.");
    }
}

package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 수료 상태를 표현하는 enum입니다.
 */
public enum ReviewCompletionStatus implements ReviewSurveyOption {
    COMPLETED("completed", "수료"),
    ONGOING("ongoing", "수강 중"),
    DROPOUT("dropout", "중도 포기");

    private final String value;
    private final String label;

    ReviewCompletionStatus(String value, String label) {
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
    public static ReviewCompletionStatus from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 수료 상태입니다.");
    }
}

package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 수료 후 6개월 내 취업 상태를 표현하는 enum입니다.
 */
public enum ReviewEmploymentStatus implements ReviewSurveyOption {
    EMPLOYED("employed", "취업"),
    PREPARING("preparing", "준비중");

    private final String value;
    private final String label;

    ReviewEmploymentStatus(String value, String label) {
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
    public static ReviewEmploymentStatus from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 취업 상태입니다.");
    }
}

package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 수업 진도 속도를 표현하는 enum입니다.
 */
public enum ReviewProgressSpeed implements ReviewSurveyOption {
    SLOW("slow", "느림"),
    MODERATE("moderate", "적당"),
    FAST("fast", "빠름");

    private final String value;
    private final String label;

    ReviewProgressSpeed(String value, String label) {
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
    public static ReviewProgressSpeed from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 진도 속도입니다.");
    }
}

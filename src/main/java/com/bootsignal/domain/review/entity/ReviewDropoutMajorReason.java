package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 중도 포기 대분류 사유를 표현하는 enum입니다.
 */
public enum ReviewDropoutMajorReason implements ReviewSurveyOption {
    DIFFICULTY("difficulty", "난이도"),
    SCHEDULE("schedule", "시간/일정"),
    CAREER("career", "진로 변경");

    private final String value;
    private final String label;

    ReviewDropoutMajorReason(String value, String label) {
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
    public static ReviewDropoutMajorReason from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 중도 포기 사유입니다.");
    }
}

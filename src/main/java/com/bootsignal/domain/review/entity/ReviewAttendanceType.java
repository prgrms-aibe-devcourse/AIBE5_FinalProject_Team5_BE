package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 수강 형태를 표현하는 enum입니다.
 */
public enum ReviewAttendanceType implements ReviewSurveyOption {
    ONLINE("online", "온라인"),
    OFFLINE("offline", "오프라인"),
    HYBRID("hybrid", "혼합");

    private final String value;
    private final String label;

    ReviewAttendanceType(String value, String label) {
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
    public static ReviewAttendanceType from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 수강 형태입니다.");
    }
}

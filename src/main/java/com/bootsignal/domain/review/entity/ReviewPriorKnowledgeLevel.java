package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 수강 전 선수 지식 수준을 표현하는 enum입니다.
 */
public enum ReviewPriorKnowledgeLevel implements ReviewSurveyOption {
    NON_MAJOR("non_major", "비전공", "#5C6AC4"),
    MAJOR("major", "전공", "#E88EB0"),
    WORKING("working", "현직", "#8BB4D2");

    private final String value;
    private final String label;
    private final String color;

    ReviewPriorKnowledgeLevel(String value, String label, String color) {
        this.value = value;
        this.label = label;
        this.color = color;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    @JsonCreator
    public static ReviewPriorKnowledgeLevel from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 선수 지식 수준입니다.");
    }
}

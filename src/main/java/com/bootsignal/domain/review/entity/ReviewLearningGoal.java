package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 사용자의 학습 목적을 표현하는 enum입니다.
 */
public enum ReviewLearningGoal implements ReviewSurveyOption {
    EMPLOYMENT("employment", "취업"),
    CAREER_CHANGE("career_change", "이직"),
    PORTFOLIO("portfolio", "포트폴리오"),
    STARTUP("startup", "창업"),
    ETC("etc", "기타");

    private final String value;
    private final String label;

    ReviewLearningGoal(String value, String label) {
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
    public static ReviewLearningGoal from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 학습 목적입니다.");
    }
}

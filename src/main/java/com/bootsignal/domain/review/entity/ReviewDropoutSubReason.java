package com.bootsignal.domain.review.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 인증 리뷰 설문에서 중도 포기 세부 사유를 표현하는 enum입니다.
 */
public enum ReviewDropoutSubReason implements ReviewSurveyOption {
    TOO_HARD("too_hard", "강의 난이도가 높음", ReviewDropoutMajorReason.DIFFICULTY),
    BASE_LACK("base_lack", "기초 지식 부족", ReviewDropoutMajorReason.DIFFICULTY),
    WORK_CONFLICT("work_conflict", "업무 병행 어려움", ReviewDropoutMajorReason.SCHEDULE),
    PERSONAL_SCHEDULE("personal_schedule", "개인 일정 충돌", ReviewDropoutMajorReason.SCHEDULE),
    CHANGE_GOAL("change_goal", "진로 목표 변경", ReviewDropoutMajorReason.CAREER),
    OTHER_PROGRAM("other_program", "타 교육 선택", ReviewDropoutMajorReason.CAREER);

    private final String value;
    private final String label;
    private final ReviewDropoutMajorReason majorReason;

    ReviewDropoutSubReason(String value, String label, ReviewDropoutMajorReason majorReason) {
        this.value = value;
        this.label = label;
        this.majorReason = majorReason;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public ReviewDropoutMajorReason majorReason() {
        return majorReason;
    }

    public boolean belongsTo(ReviewDropoutMajorReason majorReason) {
        return this.majorReason == majorReason;
    }

    @JsonCreator
    public static ReviewDropoutSubReason from(String value) {
        return ReviewSurveyOptionParser.parse(values(), value, "지원하지 않는 중도 포기 세부 사유입니다.");
    }
}

package com.bootsignal.domain.course.dto;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;

import java.util.Arrays;

/**
 * 과정 목록과 메인 인기 과정 조회에서 사용할 정렬 조건을 정의합니다.
 * popular은 북마크 수가 많은 아직 시작하지 않은 과정 조회에 사용합니다.
 */
public enum CourseSort {
    LATEST("latest"),
    POPULAR("popular"),
    SATISFACTION("satisfaction"),
    EMPLOYMENT_RATE("employmentRate"),
    DEADLINE("deadline");

    private final String value;

    CourseSort(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static CourseSort from(String sort) {
        if (sort == null || sort.isBlank()) {
            return LATEST;
        }

        return Arrays.stream(values())
                .filter(item -> item.value.equalsIgnoreCase(sort))
                .findFirst()
                .orElseThrow(() -> new BootSignalException(ErrorCode.INVALID_PAGE_PARAMETER));
    }
}

package com.bootsignal.domain.course.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

/**
 * 과정 목록 정렬 파라미터가 API 입력 문자열에서 안전하게 변환되는지 검증합니다.
 */
class CourseSortTest {

    @Test
    void fromReturnsLatestWhenSortIsBlank() {
        assertThat(CourseSort.from(null)).isEqualTo(CourseSort.LATEST);
        assertThat(CourseSort.from("")).isEqualTo(CourseSort.LATEST);
    }

    @Test
    void fromReturnsMatchingSortIgnoringCase() {
        assertThat(CourseSort.from("popular")).isEqualTo(CourseSort.POPULAR);
        assertThat(CourseSort.from("employmentRate")).isEqualTo(CourseSort.EMPLOYMENT_RATE);
        assertThat(CourseSort.from("DEADLINE")).isEqualTo(CourseSort.DEADLINE);
    }

    @Test
    void fromThrowsInvalidPageParameterWhenSortIsUnsupported() {
        assertThatThrownBy(() -> CourseSort.from("unsupported"))
                .isInstanceOf(BootSignalException.class)
                .extracting(exception -> ((BootSignalException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_PAGE_PARAMETER);
    }
}

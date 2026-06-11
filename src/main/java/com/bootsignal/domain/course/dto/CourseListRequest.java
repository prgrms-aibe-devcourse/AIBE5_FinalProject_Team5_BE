package com.bootsignal.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CourseListRequest(
        String keyword,
        String trngAreaCd,
        String ncsCd,

        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하이어야 합니다.")
        Integer size
) {
    // page와 size가 null이거나 기본값이 누락되었을 때 방어 코드 적용
    public CourseListRequest {
        if (page == null) page = 0;
        if (size == null || size == 0) size = 20;
    }
}

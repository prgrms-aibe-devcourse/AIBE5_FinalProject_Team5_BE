package com.bootsignal.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 과정 목록 조회 API의 검색, 필터, 정렬, 페이지 요청 값을 담습니다.
 */
public record CourseListRequest(
        String keyword,

        /**
         * 지역 대분류 코드 (앞 2자리)
         */
        String trngAreaCd,

        /**
         * 분야 카테고리 필터
         * AI / SECURITY / BIG_DATA / CLOUD / UI_UX / VR / APP_SW / OTHERS
         */
        FieldCategory fieldCategory,

        /**
         * 가격 필터 (selfPaymentAmount 기준)
         */
        PriceRange priceRange,

        /**
         * 기간 필터 (총 훈련일수 기준) WITHIN_3_MONTHS / WITHIN_6_MONTHS / OVER_6_MONTHS
         */
        DurationFilter durationFilter,

        /**
         * 정렬 조건: latest / popular / satisfaction / employmentRate / deadline
         * popular은 메인 인기 과정용으로 아직 시작하지 않은 과정을 북마크 수 기준으로 조회합니다.
         */
        String sort,

        /**
         * 페이지 번호 (0부터 시작)
         */
        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지 크기는 100 이하이어야 합니다.")
        Integer size
) {
    // page와 size가 null이거나 기본값이 누락되었을 때 방어 코드 적용
    public CourseListRequest {
        if (sort == null || sort.isBlank()) sort = CourseSort.LATEST.value();
        if (page == null) page = 0;
        if (size == null || size == 0) size = 20;
    }
}

package com.bootsignal.domain.course.dto;

/**
 * 가격 범위 필터 (selfPaymentAmount 기준)
 * - BELOW_30 : 0 ~ 300,000 이하
 * - BETWEEN_30_60 : 300,001 ~ 600,000
 * - ABOVE_60 : 600,001 이상
 */
public enum PriceRange {
    BELOW_30,
    BELOW_45,
    BELOW_60
}

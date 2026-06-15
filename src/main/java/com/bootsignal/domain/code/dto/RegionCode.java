package com.bootsignal.domain.code.dto;

/**
 * 지역 대분류 코드 응답 DTO
 * GET /api/codes/regions
 */
public record RegionCode(
        String code,
        String name
) {
}

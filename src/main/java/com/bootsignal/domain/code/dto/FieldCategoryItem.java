package com.bootsignal.domain.code.dto;

/**
 * 분야 카테고리 목록 응답 DTO
 * GET /api/codes/fields
 */
public record FieldCategoryItem(
        String category,
        String label
) {
}

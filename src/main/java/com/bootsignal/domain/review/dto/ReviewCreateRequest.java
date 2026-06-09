package com.bootsignal.domain.review.dto;

import com.bootsignal.domain.review.entity.ReviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
    @NotNull Long courseSessionId,
    @NotNull ReviewType reviewType,
    @NotNull @Min(1) @Max(5) Integer rating,
    @NotBlank String content
) {}

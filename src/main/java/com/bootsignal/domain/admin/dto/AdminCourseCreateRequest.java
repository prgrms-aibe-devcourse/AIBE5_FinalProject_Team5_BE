package com.bootsignal.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminCourseCreateRequest(
    @NotNull Long institutionId,
    @NotBlank String trprId,
    @NotBlank String title,
    @NotBlank String subTitle,
    String titleLink,
    String subTitleLink,
    String ncsCd,
    String ncsName,
    String ncsYn,
    BigDecimal courseMan,
    BigDecimal realMan,
    BigDecimal selfPaymentAmount,
    BigDecimal stdgScor,
    Integer totalTrainingDays,
    Integer totalTrainingHours,
    String trngAreaCd
) {}

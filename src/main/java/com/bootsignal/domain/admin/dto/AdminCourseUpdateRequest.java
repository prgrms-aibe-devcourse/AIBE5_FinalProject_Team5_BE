package com.bootsignal.domain.admin.dto;

import java.math.BigDecimal;

public record AdminCourseUpdateRequest(
    String title,
    String subTitle,
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

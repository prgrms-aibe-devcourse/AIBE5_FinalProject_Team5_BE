package com.bootsignal.domain.admin.dto;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.entity.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminCourseResponse(
    Long courseId,
    Long institutionId,
    String institutionName,
    String trprId,
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
    String trngAreaCd,
    CourseStatus status,
    String statusReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AdminCourseResponse from(Course course) {
        return new AdminCourseResponse(
            course.getId(),
            course.getInstitution() != null ? course.getInstitution().getId() : null,
            course.getInstitution() != null ? course.getInstitution().getInstitutionName() : null,
            course.getTrprId(),
            course.getTitle(),
            course.getSubTitle(),
            course.getTitleLink(),
            course.getSubTitleLink(),
            course.getNcsCd(),
            course.getNcsName(),
            course.getNcsYn(),
            course.getCourseMan(),
            course.getRealMan(),
            course.getSelfPaymentAmount(),
            course.getStdgScor(),
            course.getTotalTrainingDays(),
            course.getTotalTrainingHours(),
            course.getTrngAreaCd(),
            course.getStatus(),
            course.getStatusReason(),
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }
}

package com.bootsignal.domain.admin.dto;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.entity.CourseStatus;
import com.bootsignal.domain.course.entity.CourseVisibility;
import com.bootsignal.domain.course_session.entity.CourseSession;
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
    Integer courseMan,
    Integer selfPaymentAmount,
    BigDecimal stdgScor,
    Integer totalTrainingDays,
    Integer totalTrainingHours,
    String trngAreaCd,
    CourseStatus status,
    String statusReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AdminCourseResponse from(Course course, CourseSession repSession, CourseVisibility visibility) {
        CourseStatus effectiveStatus = visibility != null ? visibility.getStatus() : CourseStatus.ACTIVE;
        String reason = visibility != null ? visibility.getReason() : null;
        return new AdminCourseResponse(
            course.getId(),
            course.getInstitution() != null ? course.getInstitution().getId() : null,
            course.getInstitution() != null ? course.getInstitution().getInstitutionName() : null,
            course.getTrprId(),
            course.getTitle(),
            course.getSubTitle(),
            repSession != null ? repSession.getTitleLink() : null,
            course.getSubTitleLink(),
            course.getNcsCd(),
            course.getNcsName(),
            course.getNcsYn(),
            repSession != null ? repSession.getCourseMan() : null,
            repSession != null ? repSession.getSelfPaymentAmount() : null,
            course.getStdgScor(),
            repSession != null ? repSession.getTotalTrainingDays() : null,
            repSession != null ? repSession.getTotalTrainingHours() : null,
            course.getTrngAreaCd(),
            effectiveStatus,
            reason,
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }
}

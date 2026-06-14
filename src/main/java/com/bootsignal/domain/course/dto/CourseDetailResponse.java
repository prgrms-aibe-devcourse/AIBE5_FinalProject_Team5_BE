package com.bootsignal.domain.course.dto;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.institution.dto.InstitutionResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseDetailResponse(
        Long id,
        String trprId,
        String title,
        String ncsCd,
        String ncsName,
        String ncsYn,
        Integer courseMan,
        Integer selfPaymentAmount,
        BigDecimal stdgScor,
        Integer totalTrainingDays,
        Integer totalTrainingHours,
        String trngAreaCd,
        String trainingTargetRequirements,
        String trainingGoal,
        String titleLink,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        InstitutionResponse institution
) {
    public static CourseDetailResponse from(Course course) {
        return from(course, null);
    }

    public static CourseDetailResponse from(Course course, CourseSession repSession) {
        InstitutionResponse institutionResponse = course.getInstitution() != null
                ? InstitutionResponse.from(course.getInstitution())
                : null;
        return new CourseDetailResponse(
                course.getId(),
                course.getTrprId(),
                course.getTitle(),
                course.getNcsCd(),
                course.getNcsName(),
                course.getNcsYn(),
                repSession != null ? repSession.getCourseMan() : null,
                repSession != null ? repSession.getSelfPaymentAmount() : null,
                course.getStdgScor(),
                repSession != null ? repSession.getTotalTrainingDays() : null,
                repSession != null ? repSession.getTotalTrainingHours() : null,
                course.getTrngAreaCd(),
                course.getTrainingTargetRequirements(),
                course.getTrainingGoal(),
                repSession != null ? repSession.getTitleLink() : null,
                course.getCreatedAt(),
                course.getUpdatedAt(),
                institutionResponse
        );
    }
}

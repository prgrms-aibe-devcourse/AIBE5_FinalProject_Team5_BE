package com.bootsignal.domain.course.dto;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;

import java.math.BigDecimal;

public record CourseListResponse(
        Long id,
        String trprId,
        String title,
        String institutionName,
        String trngAreaCd,
        Integer courseMan,
        Integer selfPaymentAmount,
        BigDecimal stdgScor,
        Integer totalTrainingDays,
        Integer totalTrainingHours,
        String ncsName,
        String profileImageUrl
) {
    public static CourseListResponse from(Course course) {
        return from(course, null);
    }

    public static CourseListResponse from(Course course, CourseSession repSession) {
        String profileImageUrl = course.getInstitution() != null
                ? course.getInstitution().getProfileImageUrl()
                : null;
        return new CourseListResponse(
                course.getId(),
                course.getTrprId(),
                course.getTitle(),
                course.getSubTitle(),       // 기관명은 subTitle에 비정규화됨
                course.getTrngAreaCd(),
                repSession != null ? repSession.getCourseMan() : null,
                repSession != null ? repSession.getSelfPaymentAmount() : null,
                course.getStdgScor(),
                repSession != null ? repSession.getTotalTrainingDays() : null,
                repSession != null ? repSession.getTotalTrainingHours() : null,
                course.getNcsName(),
                profileImageUrl
        );
    }
}

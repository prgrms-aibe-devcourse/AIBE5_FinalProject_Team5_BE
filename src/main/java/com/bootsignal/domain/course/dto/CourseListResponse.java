package com.bootsignal.domain.course.dto;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseListResponse(
        Long id,
        Long courseId,
        Long courseSessionId,
        String trprId,
        Integer trprDegr,
        String title,
        String institutionName,
        String trngAreaCd,
        Integer courseMan,
        Integer selfPaymentAmount,
        BigDecimal stdgScor,
        Integer totalTrainingDays,
        Integer totalTrainingHours,
        String ncsName,
        String profileImageUrl,
        LocalDate traStartDate,
        LocalDate traEndDate,
        String eiEmplRate3,
        String eiEmplRate6,
        BigDecimal reviewRating,
        BigDecimal employmentRate
) {
    // 12-인자 생성자 (기존 코드 하위 호환성 유지)
    public CourseListResponse(
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
        this(
                id,
                id,
                null,
                trprId,
                null,
                title,
                institutionName,
                trngAreaCd,
                courseMan,
                selfPaymentAmount,
                stdgScor,
                totalTrainingDays,
                totalTrainingHours,
                ncsName,
                profileImageUrl,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static CourseListResponse from(Course course, CourseSession repSession, BigDecimal reviewRating) {
        String profileImageUrl = course.getInstitution() != null
                ? course.getInstitution().getProfileImageUrl()
                : null;
        // id는 세션이 있으면 세션 PK, 없으면 과정 PK — from(CourseSession, BigDecimal) 와 동일한 규칙
        Long id = repSession != null ? repSession.getId() : course.getId();
        return new CourseListResponse(
                id,
                course.getId(),
                repSession != null ? repSession.getId() : null,
                course.getTrprId(),
                repSession != null ? repSession.getTrprDegr() : null,
                course.getTitle(),
                course.getSubTitle(),       // 기관명은 subTitle에 비정규화됨
                course.getTrngAreaCd(),
                repSession != null ? repSession.getCourseMan() : null,
                repSession != null ? repSession.getSelfPaymentAmount() : null,
                course.getStdgScor(),
                repSession != null ? repSession.getTotalTrainingDays() : null,
                repSession != null ? repSession.getTotalTrainingHours() : null,
                course.getNcsName(),
                profileImageUrl,
                repSession != null ? repSession.getTraStartDate() : null,
                repSession != null ? repSession.getTraEndDate() : null,
                repSession != null ? repSession.getEiEmplRate3() : null,
                repSession != null ? repSession.getEiEmplRate6() : null,
                reviewRating,
                repSession != null ? repSession.getEmploymentRate() : null
        );
    }

    public static CourseListResponse from(CourseSession session, BigDecimal reviewRating) {
        Course course = session.getCourse();
        String profileImageUrl = (course != null && course.getInstitution() != null)
                ? course.getInstitution().getProfileImageUrl()
                : null;
        return new CourseListResponse(
                session.getId(),
                course != null ? course.getId() : null,
                session.getId(),
                session.getTrprId(),
                session.getTrprDegr(),
                course != null ? course.getTitle() : null,
                course != null ? course.getSubTitle() : null,
                course != null ? course.getTrngAreaCd() : null,
                session.getCourseMan(),
                session.getSelfPaymentAmount(),
                course != null ? course.getStdgScor() : null,
                session.getTotalTrainingDays(),
                session.getTotalTrainingHours(),
                course != null ? course.getNcsName() : null,
                profileImageUrl,
                session.getTraStartDate(),
                session.getTraEndDate(),
                session.getEiEmplRate3(),
                session.getEiEmplRate6(),
                reviewRating,
                session.getEmploymentRate()
        );
    }
}


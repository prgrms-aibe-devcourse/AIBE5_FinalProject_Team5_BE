package com.bootsignal.domain.course_session.dto;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.institution.dto.InstitutionResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CourseSessionDetailResponse(
        // CourseSession Fields
        Long id,
        Integer trprDegr,
        LocalDate traStartDate,
        LocalDate traEndDate,
        Integer yardMan,
        Integer regCourseMan,
        Integer totParMks,
        Integer finiCnt,
        String eiEmplRate3,
        String eiEmplRate6,
        String wkendSe,
        Integer selectedTraineeCount,
        Integer recruitmentCount,
        Integer confirmedTraineeCount,
        BigDecimal employmentRate,
        String titleLink,

        // Course Fields
        Long courseId,
        String trprId,
        String title,
        String subTitle,
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
        String trainingTargetRequirements,
        String trainingGoal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // Institution Fields
        InstitutionResponse institution
) {
    public static CourseSessionDetailResponse from(CourseSession session) {
        Course course = session.getCourse();
        InstitutionResponse institutionResponse = (course != null && course.getInstitution() != null)
                ? InstitutionResponse.from(course.getInstitution())
                : null;

        return new CourseSessionDetailResponse(
                session.getId(),
                session.getTrprDegr(),
                session.getTraStartDate(),
                session.getTraEndDate(),
                session.getYardMan(),
                session.getRegCourseMan(),
                session.getTotParMks(),
                session.getFiniCnt(),
                session.getEiEmplRate3(),
                session.getEiEmplRate6(),
                session.getWkendSe(),
                session.getSelectedTraineeCount(),
                session.getRecruitmentCount(),
                session.getConfirmedTraineeCount(),
                session.getEmploymentRate(),
                session.getTitleLink(),

                course != null ? course.getId() : null,
                course != null ? course.getTrprId() : session.getTrprId(),
                course != null ? course.getTitle() : null,
                course != null ? course.getSubTitle() : null,
                course != null ? course.getSubTitleLink() : null,
                course != null ? course.getNcsCd() : null,
                course != null ? course.getNcsName() : null,
                course != null ? course.getNcsYn() : null,
                session.getCourseMan(),
                session.getSelfPaymentAmount(),
                course != null ? course.getStdgScor() : null,
                session.getTotalTrainingDays(),
                session.getTotalTrainingHours(),
                course != null ? course.getTrngAreaCd() : null,
                course != null ? course.getTrainingTargetRequirements() : null,
                course != null ? course.getTrainingGoal() : null,
                course != null ? course.getCreatedAt() : null,
                course != null ? course.getUpdatedAt() : null,
                institutionResponse
        );
    }
}

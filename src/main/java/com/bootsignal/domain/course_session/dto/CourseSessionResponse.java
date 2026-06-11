package com.bootsignal.domain.course_session.dto;

import com.bootsignal.domain.course_session.entity.CourseSession;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseSessionResponse(
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
        Integer courseMan,
        Integer selfPaymentAmount,
        Integer totalTrainingDays,
        Integer totalTrainingHours
) {
    public static CourseSessionResponse from(CourseSession session) {
        return new CourseSessionResponse(
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
                session.getCourseMan(),
                session.getSelfPaymentAmount(),
                session.getTotalTrainingDays(),
                session.getTotalTrainingHours()
        );
    }
}

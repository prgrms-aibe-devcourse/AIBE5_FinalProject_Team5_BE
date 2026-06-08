package com.bootsignal.domain.course_session.entity;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 훈련과정 회차 / 원본: trprDegr
    private Integer trprDegr;

    // 훈련 시작일 / 원본: traStartDate
    private LocalDate traStartDate;

    // 훈련 종료일 / 원본: traEndDate
    private LocalDate traEndDate;

    // 정원 / 원본: yardMan
    private Integer yardMan;

    // 수강신청 인원 / 원본: regCourseMan
    private Integer regCourseMan;

    // 실제 수강인원 / 원본: totParMks
    private Integer totParMks;

    // 수료인원 / 원본: finiCnt
    private Integer finiCnt;

    // 고용보험 3개월 취업률 / 원본: eiEmplRate3
    private String eiEmplRate3;

    // 고용보험 6개월 취업률 / 원본: eiEmplRate6
    private String eiEmplRate6;

    // 주말/주중 구분 / 원본: wkendSe
    private String wkendSe;

    // 과정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    public void updateFromRaw(LocalDate traStartDate, LocalDate traEndDate,
                              Integer yardMan, Integer regCourseMan,
                              Integer totParMks, Integer finiCnt,
                              String eiEmplRate3, String eiEmplRate6,
                              String wkendSe) {
        this.traStartDate = traStartDate;
        this.traEndDate = traEndDate;
        this.yardMan = yardMan;
        this.regCourseMan = regCourseMan;
        this.totParMks = totParMks;
        this.finiCnt = finiCnt;
        this.eiEmplRate3 = eiEmplRate3;
        this.eiEmplRate6 = eiEmplRate6;
        this.wkendSe = wkendSe;
    }
}

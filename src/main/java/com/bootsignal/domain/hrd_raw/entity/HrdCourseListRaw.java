package com.bootsignal.domain.hrd_raw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 고용24 목록 API(310L01) 응답 원본 저장 엔티티.
 * API 응답 필드를 가능한 한 원본 그대로 String으로 저장
 */
@Entity
@Table(
        name = "hrd_course_list_raw",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trpr_id", "trpr_degr"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HrdCourseListRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 훈련과정 ID
    @Column(name = "trpr_id", nullable = false)
    private String trprId;

    // 훈련과정 회차
    @Column(name = "trpr_degr", nullable = false)
    private Integer trprDegr;

    // 훈련기관 ID (추후 상세 API 호출 시 srchTorgId로 사용될 가능성 대비)
    private String trainstCstmrId;

    // 과정명
    private String title;

    // 훈련기관명
    private String subTitle;

    // 과정 상세 링크
    @Column(length = 1000)
    private String titleLink;

    // 훈련기관 상세 링크
    @Column(length = 1000)
    private String subTitleLink;

    // NCS 코드
    private String ncsCd;

    // 훈련비
    private String courseMan;

    // 정원
    private String yardMan;

    // 훈련 시작일
    private String traStartDate;

    // 훈련 종료일
    private String traEndDate;

    // 훈련기관 코드
    private String instCd;

    // 주소
    private String address;

    // 훈련지역 코드
    private String trngAreaCd;

    // 실제 훈련비
    private String realMan;

    // 만족도 점수
    private String stdgScor;

    // 훈련 대상/유형 코드 (TRAIN_TARGET_CD)
    private String trainTargetCd;

    // 수집 일시
    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    /**
     * 중복 데이터 수집 시 값 갱신을 위한 편의 메서드 (Upsert용)
     */
    public void updateFromApi(String title, String subTitle, String titleLink, String subTitleLink,
                              String ncsCd, String courseMan, String yardMan, String traStartDate,
                              String traEndDate, String instCd, String address, String trngAreaCd,
                              String realMan, String stdgScor, String trainstCstmrId, String trainTargetCd) {
        this.title = title;
        this.subTitle = subTitle;
        this.titleLink = titleLink;
        this.subTitleLink = subTitleLink;
        this.ncsCd = ncsCd;
        this.courseMan = courseMan;
        this.yardMan = yardMan;
        this.traStartDate = traStartDate;
        this.traEndDate = traEndDate;
        this.instCd = instCd;
        this.address = address;
        this.trngAreaCd = trngAreaCd;
        this.realMan = realMan;
        this.stdgScor = stdgScor;
        this.trainstCstmrId = trainstCstmrId;
        this.trainTargetCd = trainTargetCd;
        this.fetchedAt = LocalDateTime.now();
    }
}

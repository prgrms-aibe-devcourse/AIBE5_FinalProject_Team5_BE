package com.bootsignal.domain.course.entity;

import com.bootsignal.domain.institution.entity.Institution;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 훈련과정 ID / 원본: trprId
    @Column(nullable = false, unique = true)
    private String trprId;

    // 과정명 / 원본: title
    @Column(nullable = false)
    private String title;

    // 기관명 / 원본: subtitle
    @Column(nullable = false)
    private String subTitle;

    // 과정 상세 링크 / 원본: titleLink
    @Column(length = 1000)
    private String titleLink;

    // 기관 상세 링크 / 원본: subTitleLink
    @Column(length = 1000)
    private String subTitleLink;

    // NCS 코드 / 원본: ncsCd
    private String ncsCd;

    // NCS 명 / 원본: ncsNm
    private String ncsName;

    // NCS 여부 / 원본: ncsYn
    private String ncsYn;

    // 훈련비 / 원본: courseMan
    private BigDecimal courseMan;

    // 실제 훈련비 / 원본: realMan
    private BigDecimal realMan;

    // 본인 부담금 / 원본: tgcrGnrlTrneOwepAllt
    private BigDecimal selfPaymentAmount;

    // 만족도 점수 / 원본: stdgScor
    private BigDecimal stdgScor;

    // 총 훈련일수 / 원본: trDcnt
    private Integer totalTrainingDays;

    // 총 훈련시간 / 원본: trtm
    private Integer totalTrainingHours;

    // 훈련지역 코드 / 원본: trngAreaCd
    private String trngAreaCd;

    // 훈련대상자요건 (HTML 크롤링)
    @Column(columnDefinition = "TEXT")
    private String trainingTargetRequirements;

    // 훈련목표 (HTML 크롤링)
    @Column(columnDefinition = "TEXT")
    private String trainingGoal;

    // HTML 크롤링 수행 시각 (UTC)
    private Instant crawledAt;

    // 소속 기관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    // 노출 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.ACTIVE;

    // 상태 변경 사유
    private String statusReason;

    public void changeStatus(CourseStatus status, String reason) {
        this.status = status;
        this.statusReason = reason;
    }

    public void updateFromRaw(String title, String subTitle, String titleLink, String subTitleLink,
                              String ncsCd, String ncsName, String ncsYn,
                              BigDecimal courseMan, BigDecimal realMan, BigDecimal selfPaymentAmount,
                              BigDecimal stdgScor, Integer totalTrainingDays, Integer totalTrainingHours,
                              String trngAreaCd, Institution institution) {
        this.title = title;
        this.subTitle = subTitle;
        this.titleLink = titleLink;
        this.subTitleLink = subTitleLink;
        this.ncsCd = ncsCd;
        this.ncsName = ncsName;
        this.ncsYn = ncsYn;
        this.courseMan = courseMan;
        this.realMan = realMan;
        this.selfPaymentAmount = selfPaymentAmount;
        this.stdgScor = stdgScor;
        this.totalTrainingDays = totalTrainingDays;
        this.totalTrainingHours = totalTrainingHours;
        this.trngAreaCd = trngAreaCd;
        this.institution = institution;
    }

    public void updateFromCrawl(String trainingTargetRequirements, String trainingGoal, Instant crawledAt) {
        this.trainingTargetRequirements = trainingTargetRequirements;
        this.trainingGoal = trainingGoal;
        this.crawledAt = crawledAt;
    }
}

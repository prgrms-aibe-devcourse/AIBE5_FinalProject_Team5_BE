package com.bootsignal.domain.course.entity;

import com.bootsignal.domain.institution.entity.Institution;
import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    // 소속 기관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;
}

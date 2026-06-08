package com.bootsignal.domain.hrd_raw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 고용24 상세 API(310L02) 응답 원본 저장 엔티티.
 */
@Entity
@Table(
        name = "hrd_course_detail_raw",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trpr_id", "trpr_degr"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HrdCourseDetailRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 훈련과정 ID (조인용)
    @Column(name = "trpr_id", nullable = false)
    private String trprId;

    // 훈련과정 회차 (조인용)
    @Column(name = "trpr_degr", nullable = false)
    private Integer trprDegr;

    // 훈련기관 담당자명
    private String trprChap;

    // 훈련기관 담당자 전화번호
    private String trprChapTel;

    // 훈련기관 담당자 이메일
    private String trprChapEmail;

    // NCS 여부
    private String ncsYn;

    // NCS 명
    private String ncsNm;

    // 총 훈련일수
    private String trDcnt;

    // 총 훈련시간
    private String trtm;

    // 훈련기관 홈페이지 주소
    @Column(length = 1000)
    private String hpAddr;

    // 본인 부담금
    private String tgcrGnrlTrneOwepAllt;

    // 수집 일시
    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    /**
     * 중복 데이터 수집 시 값 갱신을 위한 편의 메서드 (Upsert용)
     */
    public void updateFromApi(String trprChap, String trprChapTel, String trprChapEmail,
                              String ncsYn, String ncsNm, String trDcnt, String trtm,
                              String hpAddr, String tgcrGnrlTrneOwepAllt) {
        this.trprChap = trprChap;
        this.trprChapTel = trprChapTel;
        this.trprChapEmail = trprChapEmail;
        this.ncsYn = ncsYn;
        this.ncsNm = ncsNm;
        this.trDcnt = trDcnt;
        this.trtm = trtm;
        this.hpAddr = hpAddr;
        this.tgcrGnrlTrneOwepAllt = tgcrGnrlTrneOwepAllt;
        this.fetchedAt = LocalDateTime.now();
    }
}

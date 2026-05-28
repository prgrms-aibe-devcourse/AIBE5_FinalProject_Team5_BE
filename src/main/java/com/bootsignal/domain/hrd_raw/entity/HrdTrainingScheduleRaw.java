package com.bootsignal.domain.hrd_raw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 고용24 일정 API(310L03) 응답 원본 저장 엔티티.
 */
@Entity
@Table(
        name = "hrd_training_schedule_raw",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trpr_id", "trpr_degr"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HrdTrainingScheduleRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 훈련과정 ID (조인용)
    @Column(name = "trpr_id", nullable = false)
    private String trprId;

    // 훈련과정 회차 (조인용)
    @Column(name = "trpr_degr", nullable = false)
    private Integer trprDegr;

    // 고용보험 3개월 취업률
    private String eiEmplRate3;

    // 고용보험 6개월 취업률
    private String eiEmplRate6;

    // 실제 수강인원
    private String totParMks;

    // 수료인원
    private String finiCnt;

    // 수집 일시
    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    /**
     * 중복 데이터 수집 시 값 갱신을 위한 편의 메서드 (Upsert용)
     */
    public void updateFromApi(String eiEmplRate3, String eiEmplRate6,
                              String totParMks, String finiCnt) {
        this.eiEmplRate3 = eiEmplRate3;
        this.eiEmplRate6 = eiEmplRate6;
        this.totParMks = totParMks;
        this.finiCnt = finiCnt;
        this.fetchedAt = LocalDateTime.now();
    }
}

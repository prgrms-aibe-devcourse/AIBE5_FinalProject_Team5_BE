package com.bootsignal.domain.institution.entity;

import com.bootsignal.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Institution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 훈련기관 코드 / 원본: instCd
    @Column(nullable = false, unique = true)
    private String instCd;

    // 훈련기관명 / 원본: subTitle
    @Column(nullable = false)
    private String institutionName;

    // 주소 / 원본: address
    private String address;

    // 홈페이지 주소 / 원본: hpAddr
    private String homepageUrl;

    // 담당자명 / 원본: trprChap
    private String managerName;

    // 담당자 전화번호 / 원본: trprChapTel
    private String managerTel;

    // 담당자 이메일 / 원본: trprChapEmail
    private String managerEmail;

    // 기관 대표 사진 URL (HTML 크롤링)
    @Column(length = 1000)
    private String profileImageUrl;

    // 기관 소개 텍스트 (HTML 크롤링)
    @Column(columnDefinition = "TEXT")
    private String introduction;

    public void updateFromRaw(String institutionName, String address, String homepageUrl,
                              String managerName, String managerTel, String managerEmail) {
        this.institutionName = institutionName;
        this.address = address;
        this.homepageUrl = homepageUrl;
        this.managerName = managerName;
        this.managerTel = managerTel;
        this.managerEmail = managerEmail;
    }

    public void updateFromCrawl(String profileImageUrl, String introduction) {
        this.profileImageUrl = profileImageUrl;
        this.introduction = introduction;
    }
}
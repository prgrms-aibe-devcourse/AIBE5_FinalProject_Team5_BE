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

    // 우편번호 / 원본: zipCd
    private String zipCd;

    // 홈페이지 주소 / 원본: hpAddr
    private String homepageUrl;

    // 대표 전화번호 / 원본: telNo
    private String telNo;

    // 담당자명 / 원본: trprChap
    private String managerName;

    // 담당자 전화번호 / 원본: trprChapTel
    private String managerTel;

    // 담당자 이메일 / 원본: trprChapEmail
    private String managerEmail;

    public void updateFromRaw(String institutionName, String address, String homepageUrl,
                              String managerName, String managerTel, String managerEmail) {
        this.institutionName = institutionName;
        this.address = address;
        this.homepageUrl = homepageUrl;
        this.managerName = managerName;
        this.managerTel = managerTel;
        this.managerEmail = managerEmail;
    }
}
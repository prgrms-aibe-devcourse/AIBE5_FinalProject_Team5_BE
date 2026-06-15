package com.bootsignal.domain.verification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.unit.DataSize;

/**
 * 인증 증빙처럼 큰 이미지 파일을 업로드할 때 적용되는 multipart 파일 크기 설정을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class VerificationMultipartPropertiesTest {

    @Autowired
    private MultipartProperties multipartProperties;

    @Test
    void defaultMultipartLimitAllowsLargeImageUpload() {
        assertThat(multipartProperties.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(100));
        assertThat(multipartProperties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(120));
    }
}

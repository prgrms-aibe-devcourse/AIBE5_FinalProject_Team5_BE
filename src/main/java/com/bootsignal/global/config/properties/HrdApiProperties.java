package com.bootsignal.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**

 * @param authKey    고용24에서 발급받은 API 인증키
 * @param baseUrl    OpenAPI 공통 base URL
 * @param returnType API 응답 포맷 (JSON 예정)
 */
@ConfigurationProperties(prefix = "app.hrd.api")
public record HrdApiProperties(
        String authKey,
        String baseUrl,
        String returnType
) {
}

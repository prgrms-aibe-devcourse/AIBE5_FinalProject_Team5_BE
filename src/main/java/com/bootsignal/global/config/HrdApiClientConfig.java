package com.bootsignal.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 고용24 OpenAPI 통신용 RestClient 설정
 */
@Configuration
public class HrdApiClientConfig {

    @Bean
    public RestClient hrdRestClient(RestClient.Builder builder) {
        // 공공 API 특성상 응답이 지연될 수 있으므로 타임아웃을 여유있게 설정 (추후 재설정 예정)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 연결 10초
        factory.setReadTimeout(30000);    // 응답 대기 30초

        return builder.requestFactory(factory).build();
    }
}

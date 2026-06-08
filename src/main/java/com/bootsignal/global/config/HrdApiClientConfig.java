package com.bootsignal.global.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 고용24 OpenAPI 전용 RestClient 설정
 *
 * - Spring Batch 기반 단일 스레드 수집 환경
 * - TCP 연결 재사용을 위한 최소 Connection Pool 구성
 * - 외부 API 장애 시 무한 대기 방지를 위한 타임아웃 설정
 */
@Configuration
public class HrdApiClientConfig {
        // TCP 연결 수립 최대 대기시간
        private static final int CONNECT_TIMEOUT_SECONDS = 10;
        // 응답 대기 최대 시간
        private static final int RESPONSE_TIMEOUT_SECONDS = 30;

        @Bean
        public RestClient hrdRestClient(RestClient.Builder builder) {

                // TCP 연결 재사용을 위한 최소 Connection Pool
                PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

                connectionManager.setMaxTotal(2);
                connectionManager.setDefaultMaxPerRoute(2);

                // 연결 및 응답 타임아웃
                RequestConfig requestConfig = RequestConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                                .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                                .build();

                var httpClient = HttpClients.custom()
                                .setConnectionManager(connectionManager)
                                .setDefaultRequestConfig(requestConfig)
                                .setUserAgent("BootSignal-Work24-Client")
                                .build();

                return builder
                                .requestFactory(
                                                new HttpComponentsClientHttpRequestFactory(httpClient))
                                .build();
        }
}
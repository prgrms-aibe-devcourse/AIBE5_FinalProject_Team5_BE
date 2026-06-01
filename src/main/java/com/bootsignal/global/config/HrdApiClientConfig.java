package com.bootsignal.global.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 고용24 OpenAPI 통신용 RestClient 설정
 */
@Configuration
public class HrdApiClientConfig {

    @Bean
    public RestClient hrdRestClient(RestClient.Builder builder) {
        // TCP 연결 수립 타임아웃 (서버가 응답 없을 때 무한 대기 방지)
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .build();

        // 현재 단일 스레드 구조 (안정상 2개로 설정)
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(2);
        connectionManager.setDefaultMaxPerRoute(2);
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        // 풀 대기 타임아웃 + 응답 타임아웃
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return builder.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient)).build();
    }
}

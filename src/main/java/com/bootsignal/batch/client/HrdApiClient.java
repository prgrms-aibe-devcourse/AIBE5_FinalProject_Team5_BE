package com.bootsignal.batch.client;

import com.bootsignal.batch.dto.HrdCourseDetailApiResponse;
import com.bootsignal.batch.dto.HrdCourseListApiResponse;
import com.bootsignal.batch.dto.HrdTrainingScheduleApiResponse;
import com.bootsignal.global.config.properties.HrdApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 고용24 OpenAPI 3종 호출 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrdApiClient {

    private final RestClient hrdRestClient;
    private final HrdApiProperties properties;

    /**
     * 310L01 - 직업훈련과정 목록 조회
     */
    public HrdCourseListApiResponse fetchCourseList(String startDate, String endDate, int pageNum, int pageSize) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/callOpenApiSvcInfo310L01.do")
                .queryParam("authKey", properties.authKey())
                .queryParam("returnType", properties.returnType())
                .queryParam("outType", "1")
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .queryParam("srchTraStDt", startDate)
                .queryParam("srchTraEndDt", endDate)
                .queryParam("sort", "DESC") // 고정값: 내림차순
                .queryParam("sortCol", "2") // 고정값: 훈련시작일 기준
                .toUriString();

        log.debug("목록 API 호출: {}", url);

        return hrdRestClient.get()
                .uri(url)
                .retrieve()
                .body(HrdCourseListApiResponse.class);
    }

    /**
     * 310L02 - 직업훈련과정 상세 조회
     */
    public HrdCourseDetailApiResponse fetchCourseDetail(String trprId, Integer trprDegr, String torgId) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/callOpenApiSvcInfo310L02.do")
                .queryParam("authKey", properties.authKey())
                .queryParam("returnType", properties.returnType())
                .queryParam("outType", "2")
                .queryParam("srchTrprId", trprId)
                .queryParam("srchTrprDegr", trprDegr)
                .queryParam("srchTorgId", torgId != null ? torgId : "")
                .toUriString();

        return hrdRestClient.get()
                .uri(url)
                .retrieve()
                .body(HrdCourseDetailApiResponse.class);
    }

    /**
     * 310L03 - 직업훈련과정 일정(통계) 조회
     */
    public HrdTrainingScheduleApiResponse fetchTrainingSchedule(String trprId, Integer trprDegr, String torgId) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/callOpenApiSvcInfo310L03.do")
                .queryParam("authKey", properties.authKey())
                .queryParam("returnType", properties.returnType())
                .queryParam("outType", "2")
                .queryParam("srchTrprId", trprId)
                .queryParam("srchTrprDegr", trprDegr)
                .queryParam("srchTorgId", torgId != null ? torgId : "")
                .toUriString();

        return hrdRestClient.get()
                .uri(url)
                .retrieve()
                .body(HrdTrainingScheduleApiResponse.class);
    }
}

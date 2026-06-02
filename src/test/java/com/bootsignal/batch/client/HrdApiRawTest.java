package com.bootsignal.batch.client;

import com.bootsignal.global.config.properties.HrdApiProperties;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Disabled("OpenAPI 실서버 연동 검증용 테스트 CI 환경 실행 제외")
@SpringBootTest
@ActiveProfiles("local")
class HrdApiRawTest {

    @Autowired
    private RestClient hrdRestClient;

    @Autowired
    private HrdApiProperties properties;

    @Test
    void testRawJson() {
        String detailUrl = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/callOpenApiSvcInfo310L02.do")
                .queryParam("authKey", properties.authKey())
                .queryParam("returnType", properties.returnType())
                .queryParam("outType", "2")
                .queryParam("srchTrprId", "AIG20263001285883")
                .queryParam("srchTrprDegr", 5)
                .queryParam("srchTorgId", "500020062764")
                .toUriString();

        String detailJson = hrdRestClient.get().uri(detailUrl).retrieve().body(String.class);
        System.out.println("====== DETAIL RAW JSON ======");
        System.out.println(detailJson);

        String scheduleUrl = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/callOpenApiSvcInfo310L03.do")
                .queryParam("authKey", properties.authKey())
                .queryParam("returnType", properties.returnType())
                .queryParam("outType", "2")
                .queryParam("srchTrprId", "AIG20263001285883")
                .queryParam("srchTrprDegr", 5)
                .queryParam("srchTorgId", "500020062764")
                .toUriString();

        String scheduleJson = hrdRestClient.get().uri(scheduleUrl).retrieve().body(String.class);
        System.out.println("====== SCHEDULE RAW JSON ======");
        System.out.println(scheduleJson);
    }

    @Test
    void testPastDateWithKdt() {
        String startDate = "20260101";
        String endDate = "20260602";

        String listUrl = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/callOpenApiSvcInfo310L01.do")
                .queryParam("authKey", properties.authKey())
                .queryParam("returnType", properties.returnType())
                .queryParam("outType", "1")
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .queryParam("srchTraStDt", startDate)
                .queryParam("srchTraEndDt", endDate)
                .queryParam("crseTracseSe", "C0104") // KDT
                .queryParam("sort", "DESC")
                .queryParam("sortCol", "2")
                .toUriString();

        try {
            String listJson = hrdRestClient.get().uri(listUrl).retrieve().body(String.class);
            System.out.println("====== PAST DATE L01 RESPONSE ======");
            System.out.println(listJson.substring(0, Math.min(2000, listJson.length())));
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }
}

package com.bootsignal.batch.client;

import com.bootsignal.global.config.properties.HrdApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

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
}

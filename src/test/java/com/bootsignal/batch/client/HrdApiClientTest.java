package com.bootsignal.batch.client;

import com.bootsignal.batch.dto.HrdCourseDetailApiResponse;
import com.bootsignal.batch.dto.HrdCourseListApiResponse;
import com.bootsignal.batch.dto.HrdTrainingScheduleApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class HrdApiClientTest {

    @Autowired
    private HrdApiClient hrdApiClient;

    @Test
    @DisplayName("고용24 API 3종 (목록, 상세, 일정) 호출 및 JSON 파싱 테스트")
    void testHrdApi() {
        // 1. 오늘부터 3개월 뒤까지의 훈련과정 목록 조회
        String startDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = LocalDate.now().plusMonths(3).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        System.out.println("================================================");
        System.out.println("[1] 목록 API (310L01) 테스트 시작");
        System.out.println("================================================");
        
        HrdCourseListApiResponse listResponse = hrdApiClient.fetchCourseList(startDate, endDate, 1, 10);
        
        assertThat(listResponse).isNotNull();
        System.out.println("전체 검색 건수 (scn_cnt): " + listResponse.getTotalCount());
        
        if (listResponse.getCourseItems() == null || listResponse.getCourseItems().isEmpty()) {
            System.out.println("조회된 훈련과정이 없습니다. 날짜나 키를 확인하세요.");
            return;
        }

        HrdCourseListApiResponse.CourseListItem firstItem = listResponse.getCourseItems().get(0);
        System.out.println("첫 번째 과정명: " + firstItem.getTitle());
        System.out.println("훈련과정 ID (trprId): " + firstItem.getTrprId());
        System.out.println("훈련과정 회차 (trprDegr): " + firstItem.getTrprDegr());
        System.out.println("훈련기관 ID (trainstCstmrId): " + firstItem.getTrainstCstmrId());

        // 2. 알아낸 식별자로 상세 API (310L02) 호출
        System.out.println("\n================================================");
        System.out.println("[2] 상세 API (310L02) 테스트 시작");
        System.out.println("================================================");
        
        HrdCourseDetailApiResponse detailResponse = hrdApiClient.fetchCourseDetail(
                firstItem.getTrprId(), firstItem.getTrprDegr(), firstItem.getTrainstCstmrId()
        );
        
        assertThat(detailResponse).isNotNull();
        HrdCourseDetailApiResponse.InstBaseInfo baseInfo = detailResponse.getInstBaseInfo();
        
        if (baseInfo != null) {
            System.out.println("훈련기관 홈페이지: " + baseInfo.getHpAddr());
            System.out.println("담당자 이메일: " + baseInfo.getTrprChapEmail());
            System.out.println("총 훈련시간: " + baseInfo.getTrtm());
            
            if (detailResponse.getInstDetailInfo() != null) {
                System.out.println("본인부담금: " + detailResponse.getInstDetailInfo().getTgcrGnrlTrneOwepAllt());
            }
        } else {
            System.out.println("상세 API 결과가 비어있습니다.");
        }

        // 3. 알아낸 식별자로 일정/통계 API (310L03) 호출
        System.out.println("\n================================================");
        System.out.println("[3] 일정/통계 API (310L03) 테스트 시작");
        System.out.println("================================================");
        
        HrdTrainingScheduleApiResponse scheduleResponse = hrdApiClient.fetchTrainingSchedule(
                firstItem.getTrprId(), firstItem.getTrprDegr(), firstItem.getTrainstCstmrId()
        );
        
        assertThat(scheduleResponse).isNotNull();
        HrdTrainingScheduleApiResponse.ScheduleItem scheduleItem = scheduleResponse.getFirstItem();
        
        if (scheduleItem != null) {
            System.out.println("취업률 (6개월): " + scheduleItem.getEiEmplRate6());
            System.out.println("실제 수강인원: " + scheduleItem.getTotParMks());
        } else {
            System.out.println("일정 API 결과가 비어있습니다.");
        }
    }
}

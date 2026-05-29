package com.bootsignal.batch.reader;

import com.bootsignal.batch.client.HrdApiClient;
import com.bootsignal.batch.dto.HrdCourseListApiResponse;
import com.bootsignal.batch.dto.HrdCourseListApiResponse.CourseListItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.List;

/**
 * 목록 API(310L01)를 페이지 단위로 호출하여 CourseListItem을 1건씩 반환하는 커스텀 Reader.
 * API는 한 번에 최대 100건을 반환하므로, 내부 버퍼에 담아 1건씩 꺼내주는 방식으로 동작한다.
 */
@Slf4j
public class CourseListPagingReader implements ItemReader<CourseListItem> {

    private final HrdApiClient hrdApiClient;
    private final String startDate;
    private final String endDate;
    private static final int PAGE_SIZE = 100;

    private int currentPage = 1;
    private final List<CourseListItem> buffer = new ArrayList<>();
    private int bufferIndex = 0;
    private boolean finished = false;

    public CourseListPagingReader(HrdApiClient hrdApiClient, String startDate, String endDate) {
        this.hrdApiClient = hrdApiClient;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public CourseListItem read() {
        if (finished) return null;

        // 버퍼를 다 소진했으면 다음 페이지 호출
        if (bufferIndex >= buffer.size()) {
            fetchNextPage();
        }

        // 더 이상 데이터가 없으면 종료 (null 반환 = Reader 종료 시그널)
        if (buffer.isEmpty() || bufferIndex >= buffer.size()) {
            finished = true;
            return null;
        }

        return buffer.get(bufferIndex++);
    }

    private void fetchNextPage() {
        buffer.clear();
        bufferIndex = 0;

        try {
            HrdCourseListApiResponse response =
                    hrdApiClient.fetchCourseList(startDate, endDate, currentPage, PAGE_SIZE);

            if (response != null && !response.getCourseItems().isEmpty()) {
                buffer.addAll(response.getCourseItems());
                log.info("목록 API 페이지 {} 조회 완료: {} 건 (전체 {} 건)",
                        currentPage, buffer.size(), response.getTotalCount());
                currentPage++;
            }
        } catch (Exception e) {
            log.error("목록 API 호출 실패 (page={}): {}", currentPage, e.getMessage());
        }
    }
}

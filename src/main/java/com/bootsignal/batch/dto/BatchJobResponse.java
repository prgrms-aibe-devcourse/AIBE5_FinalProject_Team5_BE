package com.bootsignal.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

// 배치 Job 실행 결과 응답 DTO
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchJobResponse {

    private final String jobName;
    private final String status;
    private final Long jobId;
    private final String startDate;
    private final String endDate;
    private final String errorMessage;

    /** 날짜 범위가 있는 Job 성공 응답 (수집 Job) */
    public static BatchJobResponse success(String jobName, String status, Long jobId,
                                           String startDate, String endDate) {
        return builder()
                .jobName(jobName).status(status).jobId(jobId)
                .startDate(startDate).endDate(endDate)
                .build();
    }

    // 날짜 범위가 없는 Job 성공 응답 (정제 Job)
    public static BatchJobResponse success(String jobName, String status, Long jobId) {
        return builder()
                .jobName(jobName).status(status).jobId(jobId)
                .build();
    }

    /** 실패 응답 */
    public static BatchJobResponse error(String jobName, String errorMessage) {
        return builder()
                .jobName(jobName).status("FAILED").errorMessage(errorMessage)
                .build();
    }
}

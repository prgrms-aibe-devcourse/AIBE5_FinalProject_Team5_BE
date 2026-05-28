package com.bootsignal.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드는 무시하여 JSON 구조 변경 대비
public class HrdCourseListApiResponse {

    @JsonProperty("scn_cnt")
    private Integer totalCount;

    @JsonProperty("srchList")
    private List<CourseListItem> courseItems;

    public List<CourseListItem> getCourseItems() {
        return courseItems != null ? courseItems : Collections.emptyList();
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CourseListItem {
        @JsonProperty("trprId") private String trprId;
        @JsonProperty("trprDegr") private Integer trprDegr;
        @JsonProperty("trainstCstmrId") private String trainstCstmrId;
        @JsonProperty("title") private String title;
        @JsonProperty("subTitle") private String subTitle;
        @JsonProperty("titleLink") private String titleLink;
        @JsonProperty("subTitleLink") private String subTitleLink;
        @JsonProperty("ncsCd") private String ncsCd;
        @JsonProperty("courseMan") private String courseMan;
        @JsonProperty("yardMan") private String yardMan;
        @JsonProperty("traStartDate") private String traStartDate;
        @JsonProperty("traEndDate") private String traEndDate;
        @JsonProperty("instCd") private String instCd;
        @JsonProperty("address") private String address;
        @JsonProperty("trngAreaCd") private String trngAreaCd;
        @JsonProperty("realMan") private String realMan;
        @JsonProperty("stdgScor") private String stdgScor;
    }
}

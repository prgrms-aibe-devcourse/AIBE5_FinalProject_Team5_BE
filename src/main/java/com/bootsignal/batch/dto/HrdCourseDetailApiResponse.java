package com.bootsignal.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HrdCourseDetailApiResponse {

    @JsonProperty("srchList")
    private List<CourseDetailItem> items;

    public CourseDetailItem getFirstItem() {
        return (items != null && !items.isEmpty()) ? items.get(0) : null;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CourseDetailItem {
        @JsonProperty("trprChap") private String trprChap;
        @JsonProperty("trprChapTel") private String trprChapTel;
        @JsonProperty("trprChapEmail") private String trprChapEmail;
        @JsonProperty("ncsYn") private String ncsYn;
        @JsonProperty("ncsNm") private String ncsNm;
        @JsonProperty("trDcnt") private String trDcnt;
        @JsonProperty("trtm") private String trtm;
        @JsonProperty("hpAddr") private String hpAddr;
        @JsonProperty("tgcrGnrlTrneOwepAllt") private String tgcrGnrlTrneOwepAllt;
    }
}

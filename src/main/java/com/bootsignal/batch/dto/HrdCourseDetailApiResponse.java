package com.bootsignal.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HrdCourseDetailApiResponse {

    @JsonProperty("inst_base_info")
    private InstBaseInfo instBaseInfo;

    @JsonProperty("inst_detail_info")
    private InstDetailInfo instDetailInfo;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstBaseInfo {
        @JsonProperty("trprChap") private String trprChap;
        @JsonProperty("trprChapTel") private String trprChapTel;
        @JsonProperty("trprChapEmail") private String trprChapEmail;
        @JsonProperty("ncsYn") private String ncsYn;
        @JsonProperty("ncsNm") private String ncsNm;
        @JsonProperty("trDcnt") private String trDcnt;
        @JsonProperty("trtm") private String trtm;
        @JsonProperty("hpAddr") private String hpAddr;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstDetailInfo {
        @JsonProperty("tgcrGnrlTrneOwepAllt") private String tgcrGnrlTrneOwepAllt;
    }
}

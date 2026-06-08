package com.bootsignal.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HrdTrainingScheduleApiResponse {

    @JsonProperty("scn_list")
    private List<ScheduleItem> items;

    public ScheduleItem getFirstItem() {
        return (items != null && !items.isEmpty()) ? items.get(0) : null;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScheduleItem {
        @JsonProperty("eiEmplRate3") private String eiEmplRate3;
        @JsonProperty("eiEmplRate6") private String eiEmplRate6;
        @JsonProperty("totParMks") private String totParMks;
        @JsonProperty("finiCnt") private String finiCnt;
    }
}

package com.bootsignal.domain.code.service;

import com.bootsignal.domain.code.dto.RegionCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 지역 코드 서비스
 * - region-codes.json 을 앱 시작 시 1회 파싱하여 메모리에 캐싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionCodeService {

    private final ObjectMapper objectMapper;

    private List<RegionCode> regionCodes = Collections.emptyList();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("data/region-codes.json");
            List<RegionCode> loaded = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<RegionCode>>() {}
            );
            this.regionCodes = Collections.unmodifiableList(loaded);
            log.info("[RegionCodeService] 지역 코드 {}개 로드 완료", regionCodes.size());
        } catch (IOException e) {
            log.error("[RegionCodeService] region-codes.json 로드 실패", e);
        }
    }

    public List<RegionCode> getRegionCodes() {
        return regionCodes;
    }
}

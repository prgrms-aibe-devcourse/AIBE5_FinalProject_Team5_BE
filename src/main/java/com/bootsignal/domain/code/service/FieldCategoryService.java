package com.bootsignal.domain.code.service;

import com.bootsignal.domain.code.dto.FieldCategoryItem;
import com.bootsignal.domain.course.dto.FieldCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 분야 카테고리 코드 서비스
 * - field-categories.json 을 앱 시작 시 1회 파싱하여 메모리에 캐싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldCategoryService {

    private final ObjectMapper objectMapper;

    /** 카테고리 → NCS 코드 Set 매핑 */
    private Map<FieldCategory, Set<String>> categoryCodesMap = Collections.emptyMap();

    /** 7개 카테고리에 속한 전체 NCS 코드 Set (OTHERS 처리용) */
    private Set<String> allCategorizedCodes = Collections.emptySet();

    /** 프론트 응답용 카테고리 목록 */
    private List<FieldCategoryItem> categoryItems = Collections.emptyList();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("data/field-categories.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());

            Map<FieldCategory, Set<String>> codeMap = new EnumMap<>(FieldCategory.class);
            Set<String> allCodes = new HashSet<>();
            List<FieldCategoryItem> items = new ArrayList<>();

            for (JsonNode node : root) {
                String categoryStr = node.get("category").asText();
                String label = node.get("label").asText();
                FieldCategory category = FieldCategory.valueOf(categoryStr);

                Set<String> codes = objectMapper.convertValue(
                        node.get("codes"),
                        new TypeReference<Set<String>>() {}
                );

                codeMap.put(category, codes);
                allCodes.addAll(codes);
                items.add(new FieldCategoryItem(categoryStr, label));
            }

            // OTHERS 항목은 JSON에 없으므로 수동 추가
            items.add(new FieldCategoryItem("OTHERS", "기타"));

            this.categoryCodesMap = Collections.unmodifiableMap(codeMap);
            this.allCategorizedCodes = Collections.unmodifiableSet(allCodes);
            this.categoryItems = Collections.unmodifiableList(items);

            log.info("[FieldCategoryService] NCS 카테고리 {}개 로드 완료, 전체 코드 {}개",
                    codeMap.size(), allCodes.size());
        } catch (IOException e) {
            log.error("[FieldCategoryService] field-categories.json 로드 실패", e);
        }
    }

    /**
     * 특정 카테고리에 속한 NCS 코드 Set 반환
     * OTHERS인 경우 null 반환 (Specification에서 NOT IN으로 처리)
     */
    public Set<String> getCodesFor(FieldCategory category) {
        if (category == FieldCategory.OTHERS) {
            return null;
        }
        return categoryCodesMap.getOrDefault(category, Collections.emptySet());
    }

    /**
     * 7개 카테고리에 속한 전체 NCS 코드 Set 반환 (OTHERS 필터링용)
     */
    public Set<String> getAllCategorizedCodes() {
        return allCategorizedCodes;
    }

    /**
     * 프론트 응답용 카테고리 목록 반환
     */
    public List<FieldCategoryItem> getCategoryItems() {
        return categoryItems;
    }
}

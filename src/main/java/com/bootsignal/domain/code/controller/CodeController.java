package com.bootsignal.domain.code.controller;

import com.bootsignal.domain.code.dto.FieldCategoryItem;
import com.bootsignal.domain.code.dto.RegionCode;
import com.bootsignal.domain.code.service.FieldCategoryService;
import com.bootsignal.domain.code.service.RegionCodeService;
import com.bootsignal.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CodeController {

    private final FieldCategoryService fieldCategoryService;
    private final RegionCodeService regionCodeService;

    /**
     * 분야 카테고리 목록 조회
     * GET /api/codes/fields
     * 응답: [{ category: "AI", label: "인공지능" }, ...]
     */
    @GetMapping("/fields")
    public ApiResponse<List<FieldCategoryItem>> getFieldCategories() {
        return ApiResponse.success(fieldCategoryService.getCategoryItems());
    }

    /**
     * 지역 대분류 코드 목록 조회
     * GET /api/codes/regions
     * 응답: [{ code: "11", name: "서울" }, ...]
     */
    @GetMapping("/regions")
    public ApiResponse<List<RegionCode>> getRegionCodes() {
        return ApiResponse.success(regionCodeService.getRegionCodes());
    }
}

package com.bootsignal.domain.institution.controller;

import com.bootsignal.domain.institution.dto.InstitutionResponse;
import com.bootsignal.domain.institution.service.InstitutionService;
import com.bootsignal.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    /**
     * 훈련기관 상세 조회
     * GET /api/institutions/{institutionId}
     */
    @GetMapping("/{institutionId}")
    public ApiResponse<InstitutionResponse> getInstitution(
            @PathVariable Long institutionId) {
        return ApiResponse.success(institutionService.getInstitution(institutionId));
    }
}

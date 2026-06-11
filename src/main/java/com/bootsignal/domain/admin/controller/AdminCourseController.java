package com.bootsignal.domain.admin.controller;

import com.bootsignal.domain.admin.dto.AdminCourseResponse;
import com.bootsignal.domain.admin.dto.AdminCourseStatusRequest;
import com.bootsignal.domain.admin.service.AdminCourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    /** 17.4 과정 노출 상태 변경 */
    @PatchMapping("/{courseId}/status")
    public AdminCourseResponse changeStatus(
        @PathVariable Long courseId,
        @RequestBody @Valid AdminCourseStatusRequest request
    ) {
        return adminCourseService.changeStatus(courseId, request);
    }
}

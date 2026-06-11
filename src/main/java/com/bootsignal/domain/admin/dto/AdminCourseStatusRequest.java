package com.bootsignal.domain.admin.dto;

import com.bootsignal.domain.course.entity.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCourseStatusRequest(
    @NotNull CourseStatus status,
    @NotBlank String reason
) {}

package com.bootsignal.domain.admin.service;

import com.bootsignal.domain.admin.dto.AdminCourseResponse;
import com.bootsignal.domain.admin.dto.AdminCourseStatusRequest;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public AdminCourseResponse changeStatus(Long courseId, AdminCourseStatusRequest request) {
        Course course = findCourse(courseId);
        course.changeStatus(request.status(), request.reason());
        return AdminCourseResponse.from(course);
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
            .orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND, "해당 과정을 찾을 수 없습니다."));
    }
}

package com.bootsignal.domain.admin.service;

import com.bootsignal.domain.admin.dto.AdminCourseCreateRequest;
import com.bootsignal.domain.admin.dto.AdminCourseResponse;
import com.bootsignal.domain.admin.dto.AdminCourseStatusRequest;
import com.bootsignal.domain.admin.dto.AdminCourseUpdateRequest;
import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.institution.entity.Institution;
import com.bootsignal.domain.institution.repository.InstitutionRepository;
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
    private final InstitutionRepository institutionRepository;

    @Transactional
    public AdminCourseResponse create(AdminCourseCreateRequest request) {
        Institution institution = institutionRepository.findById(request.institutionId())
            .orElseThrow(() -> new BootSignalException(ErrorCode.NOT_FOUND, "해당 기관을 찾을 수 없습니다."));

        Course course = Course.builder()
            .institution(institution)
            .trprId(request.trprId())
            .title(request.title())
            .subTitle(request.subTitle())
            .titleLink(request.titleLink())
            .subTitleLink(request.subTitleLink())
            .ncsCd(request.ncsCd())
            .ncsName(request.ncsName())
            .ncsYn(request.ncsYn())
            .courseMan(request.courseMan())
            .realMan(request.realMan())
            .selfPaymentAmount(request.selfPaymentAmount())
            .stdgScor(request.stdgScor())
            .totalTrainingDays(request.totalTrainingDays())
            .totalTrainingHours(request.totalTrainingHours())
            .trngAreaCd(request.trngAreaCd())
            .build();

        return AdminCourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public AdminCourseResponse update(Long courseId, AdminCourseUpdateRequest request) {
        Course course = findCourse(courseId);
        course.adminUpdate(
            request.title(), request.subTitle(), request.titleLink(), request.subTitleLink(),
            request.ncsCd(), request.ncsName(), request.ncsYn(),
            request.courseMan(), request.realMan(), request.selfPaymentAmount(),
            request.stdgScor(), request.totalTrainingDays(), request.totalTrainingHours(),
            request.trngAreaCd()
        );
        return AdminCourseResponse.from(course);
    }

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

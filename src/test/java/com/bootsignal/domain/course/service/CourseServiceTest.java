package com.bootsignal.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bootsignal.domain.code.service.FieldCategoryService;
import com.bootsignal.domain.course.dto.CourseListRequest;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.course_session.repository.CourseSessionRepository;
import com.bootsignal.domain.crawled_review.repository.CrawledReviewRepository;
import com.bootsignal.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * 과정 목록 서비스가 요청 정렬 값을 실제 페이징 정렬 조건으로 변환하는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSessionRepository courseSessionRepository;

    @Mock
    private FieldCategoryService fieldCategoryService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CrawledReviewRepository crawledReviewRepository;

    @InjectMocks
    private CourseService courseService;

    @ParameterizedTest
    @CsvSource({
            "satisfaction, course.stdgScor, DESC",
            "employmentRate, employmentRate, DESC",
            "latest, id, DESC"
    })
    void getCoursesAppliesRequestedSort(String sort, String expectedProperty, Sort.Direction expectedDirection) {
        when(courseSessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<CourseSession>>any(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        ))
                .thenReturn(Page.empty());

        CourseListRequest request = new CourseListRequest(
                null,
                null,
                null,
                null,
                null,
                sort,
                0,
                10,
                null
        );

        courseService.getCourses(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(courseSessionRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<CourseSession>>any(),
                pageableCaptor.capture()
        );

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor(expectedProperty);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(expectedDirection);
    }

    @Test
    void getCoursesUsesQueryParameterSizeAndSpecificationOrderForPopularSort() {
        when(courseSessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<CourseSession>>any(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        ))
                .thenReturn(Page.empty());

        CourseListRequest request = new CourseListRequest(
                null,
                null,
                null,
                null,
                null,
                "popular",
                0,
                7,
                null
        );

        courseService.getCourses(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(courseSessionRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<CourseSession>>any(),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(7);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void getCoursesUsesSpecificationOrderForDeadlineSort() {
        when(courseSessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<CourseSession>>any(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        ))
                .thenReturn(Page.empty());

        CourseListRequest request = new CourseListRequest(
                null,
                null,
                null,
                null,
                null,
                "deadline",
                0,
                10,
                null
        );

        courseService.getCourses(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(courseSessionRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<CourseSession>>any(),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }
}

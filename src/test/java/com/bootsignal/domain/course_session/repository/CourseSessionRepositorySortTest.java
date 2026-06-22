package com.bootsignal.domain.course_session.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.course.entity.Course;
import com.bootsignal.domain.course.repository.CourseRepository;
import com.bootsignal.domain.course_session.entity.CourseSession;
import com.bootsignal.domain.user.entity.User;
import com.bootsignal.domain.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * 과정 회차 repository가 과정 연관 필드 정렬을 실제 JPA 쿼리로 처리하는지 검증합니다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:course_sort_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CourseSessionRepositorySortTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseSessionRepository courseSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    private int bookmarkUserSequence = 1;

    @Test
    void findAllSortsByCourseSatisfactionWithFetchJoin() {
        Course lowerCourse = saveCourse("TR-LOW", "낮은 만족도 과정", BigDecimal.valueOf(3.1));
        Course higherCourse = saveCourse("TR-HIGH", "높은 만족도 과정", BigDecimal.valueOf(4.8));
        courseSessionRepository.saveAll(List.of(
                session("TR-LOW", 1, lowerCourse),
                session("TR-HIGH", 1, higherCourse)
        ));

        Specification<CourseSession> withFetch = (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("course", JoinType.LEFT);
            }
            return cb.conjunction();
        };

        Page<CourseSession> result = courseSessionRepository.findAll(
                withFetch,
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("course.stdgScor")))
        );

        assertThat(result.getContent()).extracting(session -> session.getCourse().getTitle())
                .containsExactly("높은 만족도 과정", "낮은 만족도 과정");
    }

    @Test
    void popularSpecificationSortsByBookmarkCountAndExcludesStartedCourses() {
        LocalDate today = LocalDate.now();
        Course fewBookmarkCourse = saveCourse("TR-FEW", "북마크 적은 과정", BigDecimal.valueOf(4.9));
        Course manyBookmarkCourse = saveCourse("TR-MANY", "북마크 많은 과정", BigDecimal.valueOf(4.1));
        Course startedCourse = saveCourse("TR-STARTED", "이미 시작한 과정", BigDecimal.valueOf(5.0));

        CourseSession fewBookmarkSession = courseSessionRepository.save(
                session("TR-FEW", 1, fewBookmarkCourse, today.plusDays(10))
        );
        CourseSession manyBookmarkSession = courseSessionRepository.save(
                session("TR-MANY", 1, manyBookmarkCourse, today.plusDays(20))
        );
        CourseSession startedSession = courseSessionRepository.save(
                session("TR-STARTED", 1, startedCourse, today.minusDays(1))
        );

        saveBookmarks(fewBookmarkSession, 1);
        saveBookmarks(manyBookmarkSession, 3);
        saveBookmarks(startedSession, 5);

        Specification<CourseSession> popularSpec = Specification.allOf(
                CourseSessionSpecification.startsOnOrAfter(today),
                CourseSessionSpecification.orderByBookmarkCountDesc()
        );

        Page<CourseSession> result = courseSessionRepository.findAll(
                popularSpec,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting(session -> session.getCourse().getTitle())
                .containsExactly("북마크 많은 과정", "북마크 적은 과정");
    }

    @Test
    void deadlineSpecificationSortsUpcomingCoursesBeforeStartedCourses() {
        LocalDate today = LocalDate.now();
        Course pastCourse = saveCourse("TR-DEADLINE-PAST", "이미 시작한 과정", BigDecimal.valueOf(5.0));
        Course nearFutureCourse = saveCourse("TR-DEADLINE-NEAR", "곧 시작하는 과정", BigDecimal.valueOf(4.0));
        Course farFutureCourse = saveCourse("TR-DEADLINE-FAR", "나중에 시작하는 과정", BigDecimal.valueOf(3.0));
        Course noStartDateCourse = saveCourse("TR-DEADLINE-NO-DATE", "시작일 없는 과정", BigDecimal.valueOf(2.0));

        courseSessionRepository.saveAll(List.of(
                session("TR-DEADLINE-PAST", 1, pastCourse, today.minusDays(3)),
                session("TR-DEADLINE-NEAR", 1, nearFutureCourse, today.plusDays(1)),
                session("TR-DEADLINE-FAR", 1, farFutureCourse, today.plusDays(20)),
                session("TR-DEADLINE-NO-DATE", 1, noStartDateCourse)
        ));

        Page<CourseSession> result = courseSessionRepository.findAll(
                CourseSessionSpecification.orderByDeadlineSoon(today),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting(CourseSession::getTrprId)
                .containsExactly(
                        "TR-DEADLINE-NEAR",
                        "TR-DEADLINE-FAR",
                        "TR-DEADLINE-PAST",
                        "TR-DEADLINE-NO-DATE"
                );
    }

    private Course saveCourse(String trprId, String title, BigDecimal stdgScor) {
        return courseRepository.save(Course.builder()
                .trprId(trprId)
                .title(title)
                .subTitle("테스트 훈련기관")
                .stdgScor(stdgScor)
                .build());
    }

    private CourseSession session(String trprId, Integer trprDegr, Course course) {
        return CourseSession.builder()
                .trprId(trprId)
                .trprDegr(trprDegr)
                .course(course)
                .build();
    }

    private CourseSession session(String trprId, Integer trprDegr, Course course, LocalDate traStartDate) {
        return CourseSession.builder()
                .trprId(trprId)
                .trprDegr(trprDegr)
                .course(course)
                .traStartDate(traStartDate)
                .traEndDate(traStartDate.plusMonths(3))
                .build();
    }

    private void saveBookmarks(CourseSession courseSession, int count) {
        for (int i = 0; i < count; i++) {
            int seq = bookmarkUserSequence++;
            User user = userRepository.save(
                    User.signupLocal("bookmark-user-" + seq + "@test.com", "pw", "bm-nick-" + seq)
            );
            jdbcTemplate.update(
                    "INSERT INTO bookmark (course_session_id, user_id, start_date, end_date) VALUES (?, ?, ?, ?)",
                    courseSession.getId(),
                    user.getId(),
                    courseSession.getTraStartDate(),
                    courseSession.getTraEndDate()
            );
        }
    }
}

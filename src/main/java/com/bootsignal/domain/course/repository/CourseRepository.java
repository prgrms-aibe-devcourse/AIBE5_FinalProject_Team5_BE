package com.bootsignal.domain.course.repository;

import com.bootsignal.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByTrprId(String trprId);

    @Query("""
        select c
        from Course c
        join c.institution i
        where i.institutionName like %:keyword%
    """)
    List<Course> searchByInstitutionName(String keyword);
}

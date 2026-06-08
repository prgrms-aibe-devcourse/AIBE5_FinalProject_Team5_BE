package com.bootsignal.domain.hrd_raw.repository;

import com.bootsignal.domain.hrd_raw.entity.HrdCourseListRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrdCourseListRawRepository extends JpaRepository<HrdCourseListRaw, Long> {

    Optional<HrdCourseListRaw> findByTrprIdAndTrprDegr(String trprId, Integer trprDegr);
}

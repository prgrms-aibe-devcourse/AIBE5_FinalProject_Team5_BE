package com.bootsignal.domain.hrd_raw.repository;

import com.bootsignal.domain.hrd_raw.entity.HrdCourseDetailRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrdCourseDetailRawRepository extends JpaRepository<HrdCourseDetailRaw, Long> {

    Optional<HrdCourseDetailRaw> findByTrprIdAndTrprDegr(String trprId, Integer trprDegr);
}

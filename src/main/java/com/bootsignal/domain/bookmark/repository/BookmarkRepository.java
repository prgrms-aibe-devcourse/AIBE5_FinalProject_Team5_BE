package com.bootsignal.domain.bookmark.repository;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	boolean existsByUserIdAndCourseSessionId(Long userId, Long courseSessionId);

	Optional<Bookmark> findByUserIdAndCourseSessionId(Long userId, Long courseSessionId);
}

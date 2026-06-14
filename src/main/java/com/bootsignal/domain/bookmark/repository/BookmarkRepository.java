package com.bootsignal.domain.bookmark.repository;

import com.bootsignal.domain.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	boolean existsByUserIdAndCourseSessionId(Long userId, Long courseSessionId);

	Optional<Bookmark> findByUserIdAndCourseSessionId(Long userId, Long courseSessionId);

	@Query(
		value = """
			SELECT b FROM Bookmark b
			JOIN FETCH b.courseSession cs
			JOIN FETCH cs.course c
			LEFT JOIN FETCH c.institution i
			WHERE b.user.id = :userId
			""",
		countQuery = """
			SELECT count(b) FROM Bookmark b
			WHERE b.user.id = :userId
			"""
	)
	Page<Bookmark> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}

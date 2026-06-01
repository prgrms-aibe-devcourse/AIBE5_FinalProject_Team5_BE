package com.bootsignal.domain.post.repository;

import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query(
		value = """
			SELECT p FROM Post p
			JOIN FETCH p.user u
			LEFT JOIN FETCH p.course c
			WHERE p.deletedAt IS NULL
			  AND p.isValid = true
			  AND (:postType IS NULL OR p.postType = :postType)
			  AND (:courseId IS NULL OR (p.course IS NOT NULL AND p.course.id = :courseId))
			  AND (:keyword IS NULL OR p.title LIKE %:keyword% OR p.content LIKE %:keyword%)
			""",
		countQuery = """
			SELECT count(p) FROM Post p
			WHERE p.deletedAt IS NULL
			  AND p.isValid = true
			  AND (:postType IS NULL OR p.postType = :postType)
			  AND (:courseId IS NULL OR (p.course IS NOT NULL AND p.course.id = :courseId))
			  AND (:keyword IS NULL OR p.title LIKE %:keyword% OR p.content LIKE %:keyword%)
			"""
	)
	Page<Post> findAllActive(
		@Param("postType") PostType postType,
		@Param("courseId") Long courseId,
		@Param("keyword") String keyword,
		Pageable pageable
	);
}

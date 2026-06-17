package com.bootsignal.domain.post.repository;

import com.bootsignal.domain.post.entity.Post;
import com.bootsignal.domain.post.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 게시글 조회, 검색, 활성 게시글 존재 여부를 처리하는 JPA 저장소입니다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

	boolean existsByIdAndDeletedAtIsNullAndIsValidTrue(Long id);

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

	/**
	 * 현재 사용자가 작성한 게시글을 타입 필터와 함께 페이지 단위로 조회합니다.
	 * type 이 null 이면 전체 유형을 반환합니다.
	 */
	@Query(
		value = """
			SELECT p FROM Post p
			JOIN FETCH p.user u
			LEFT JOIN FETCH p.course c
			WHERE p.user.id = :userId
			  AND p.deletedAt IS NULL
			  AND p.isValid = true
			  AND (:postType IS NULL OR p.postType = :postType)
			""",
		countQuery = """
			SELECT count(p) FROM Post p
			WHERE p.user.id = :userId
			  AND p.deletedAt IS NULL
			  AND p.isValid = true
			  AND (:postType IS NULL OR p.postType = :postType)
			"""
	)
	Page<Post> findAllByUser(
		@Param("userId") Long userId,
		@Param("postType") PostType postType,
		Pageable pageable
	);
}


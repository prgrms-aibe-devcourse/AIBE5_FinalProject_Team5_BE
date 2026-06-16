package com.bootsignal.domain.comment.repository;

import com.bootsignal.domain.comment.entity.Comment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 댓글 저장, 단건 조회와 게시글별 활성 댓글 페이지 조회를 담당하는 JPA 저장소입니다.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query(
		value = """
			SELECT c FROM Comment c
			JOIN FETCH c.user u
			WHERE c.post.id = :postId
			  AND c.deletedAt IS NULL
			  AND c.valid = true
			""",
		countQuery = """
			SELECT count(c) FROM Comment c
			WHERE c.post.id = :postId
			  AND c.deletedAt IS NULL
			  AND c.valid = true
			"""
	)
	Page<Comment> findAllActiveByPostId(@Param("postId") Long postId, Pageable pageable);

	@Query("""
		SELECT c FROM Comment c
		JOIN FETCH c.post p
		JOIN FETCH c.user u
		WHERE c.id = :commentId
		  AND c.deletedAt IS NULL
		  AND c.valid = true
		""")
	Optional<Comment> findActiveById(@Param("commentId") Long commentId);

	/**
	 * 현재 사용자가 작성한 활성 댓글을 페이지 단위로 조회합니다.
	 */
	@Query(
		value = """
			SELECT c FROM Comment c
			JOIN FETCH c.post p
			JOIN FETCH c.user u
			WHERE c.user.id = :userId
			  AND c.deletedAt IS NULL
			  AND c.valid = true
			""",
		countQuery = """
			SELECT count(c) FROM Comment c
			WHERE c.user.id = :userId
			  AND c.deletedAt IS NULL
			  AND c.valid = true
			"""
	)
	Page<Comment> findAllByUser(
		@Param("userId") Long userId,
		Pageable pageable
	);
}


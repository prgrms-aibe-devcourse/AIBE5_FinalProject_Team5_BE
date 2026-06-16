package com.bootsignal.domain.user.repository;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회원 조회와 중복 검사를 담당하며, 계정 단위 동시성 제어가 필요한 조회에는 쓰기 잠금을 적용합니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	boolean existsByNicknameAndIdNot(String nickname, Long id);

	Optional<User> findByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.email = :email")
	Optional<User> findByEmailForUpdate(@Param("email") String email);

	Optional<User> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}

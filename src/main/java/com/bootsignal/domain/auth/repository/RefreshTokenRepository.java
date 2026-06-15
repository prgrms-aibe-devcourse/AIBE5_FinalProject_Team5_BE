package com.bootsignal.domain.auth.repository;

import com.bootsignal.domain.auth.entity.RefreshToken;
import com.bootsignal.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Refresh Token 해시를 조회하고 사용자별 활성 세션을 찾는 저장소입니다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	/**
	 * 토큰 회전 중 동시 요청이 같은 Refresh Token을 중복 사용하지 못하도록 행 잠금을 적용합니다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findAllByUserAndRevokedFalseAndReplacedFalse(User user);
}

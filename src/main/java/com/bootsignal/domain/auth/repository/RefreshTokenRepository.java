package com.bootsignal.domain.auth.repository;

import com.bootsignal.domain.auth.entity.RefreshToken;
import com.bootsignal.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Refresh Token 해시를 조회하고 사용자별 활성 세션을 찾는 저장소입니다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findAllByUserAndRevokedFalseAndReplacedFalse(User user);
}

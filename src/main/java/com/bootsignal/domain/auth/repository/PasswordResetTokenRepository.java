package com.bootsignal.domain.auth.repository;

import com.bootsignal.domain.auth.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * 비밀번호 재설정 토큰 해시를 조회하고 동시 재사용을 막기 위해 잠금을 적용하는 저장소입니다.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}

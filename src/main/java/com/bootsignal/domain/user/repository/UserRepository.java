package com.bootsignal.domain.user.repository;

import com.bootsignal.domain.user.entity.AuthProvider;
import com.bootsignal.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	boolean existsByNicknameAndIdNot(String nickname, Long id);

	Optional<User> findByEmail(String email);

	Optional<User> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}

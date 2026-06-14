package com.bootsignal.global.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.global.config.properties.TokenEncryptionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenEncryptionService 테스트")
class TokenEncryptionServiceTest {

	private TokenEncryptionService tokenEncryptionService;

	@BeforeEach
	void setUp() {
		tokenEncryptionService = new TokenEncryptionService(
			new TokenEncryptionProperties("test-token-encryption-secret-change-before-deploy-32bytes")
		);
	}

	@Test
	@DisplayName("암호화 후 복호화하면 원문과 같다")
	void encryptAndDecryptRoundTrip() {
		String plainText = "ya29.a0AfH6SMBx-example-access-token";

		String encrypted = tokenEncryptionService.encrypt(plainText);

		assertThat(encrypted).startsWith("v1:");
		assertThat(encrypted).isNotEqualTo(plainText);
		assertThat(tokenEncryptionService.decrypt(encrypted)).isEqualTo(plainText);
	}

	@Test
	@DisplayName("암호화 prefix가 없으면 legacy 평문으로 취급한다")
	void decryptReturnsLegacyPlainTextAsIs() {
		String legacyToken = "ya29.a0AfH6SMBx-legacy-plain-token";

		assertThat(tokenEncryptionService.decrypt(legacyToken)).isEqualTo(legacyToken);
	}

	@Test
	@DisplayName("null 또는 빈 문자열은 그대로 반환한다")
	void encryptAndDecryptHandleBlankValues() {
		assertThat(tokenEncryptionService.encrypt(null)).isNull();
		assertThat(tokenEncryptionService.encrypt("")).isEmpty();
		assertThat(tokenEncryptionService.decrypt(null)).isNull();
		assertThat(tokenEncryptionService.decrypt("")).isEmpty();
	}
}

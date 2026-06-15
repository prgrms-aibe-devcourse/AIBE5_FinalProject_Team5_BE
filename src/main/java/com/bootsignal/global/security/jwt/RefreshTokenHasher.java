package com.bootsignal.global.security.jwt;

import com.bootsignal.global.config.properties.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 원문이 DB에 남지 않도록 서버 비밀키 기반 HMAC 해시를 생성합니다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenHasher {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final JwtProperties jwtProperties;

	public String hash(String refreshToken) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			SecretKeySpec keySpec = new SecretKeySpec(
				jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
				HMAC_ALGORITHM
			);
			mac.init(keySpec);
			return HexFormat.of().formatHex(mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new IllegalStateException("Refresh Token 해시 생성에 실패했습니다.", exception);
		}
	}
}

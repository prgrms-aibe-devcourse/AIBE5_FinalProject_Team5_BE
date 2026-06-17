package com.bootsignal.global.security.crypto;

import com.bootsignal.global.config.properties.TokenEncryptionProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TokenEncryptionService {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final String KEY_ALGORITHM = "AES";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;
	private static final String ENCRYPTED_PREFIX = "v1:";

	private final SecretKeySpec secretKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public TokenEncryptionService(TokenEncryptionProperties tokenEncryptionProperties) {
		this.secretKey = new SecretKeySpec(deriveKey(tokenEncryptionProperties.secret()), KEY_ALGORITHM);
	}

	/* 토큰 암호화 */
	public String encrypt(String plainText) {
		if (!StringUtils.hasText(plainText)) {
			return plainText;
		}

		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

			byte[] combined = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

			return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(combined);
		} catch (Exception exception) {
			throw new IllegalStateException("토큰 암호화에 실패했습니다.", exception);
		}
	}

	/* 토큰 복호화 */
	public String decrypt(String storedValue) {
		if (!StringUtils.hasText(storedValue)) {
			return storedValue;
		}
		if (!storedValue.startsWith(ENCRYPTED_PREFIX)) {
			return storedValue;
		}

		try {
			byte[] combined = Base64.getDecoder().decode(storedValue.substring(ENCRYPTED_PREFIX.length()));
			if (combined.length <= GCM_IV_LENGTH) {
				throw new IllegalArgumentException("암호화된 토큰 형식이 올바르지 않습니다.");
			}

			byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
			byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
			byte[] decrypted = cipher.doFinal(ciphertext);
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception exception) {
			throw new IllegalStateException("토큰 복호화에 실패했습니다.", exception);
		}
	}

	/* 토큰 암호화 키 생성 */
	private byte[] deriveKey(String secret) {
		try {
			return MessageDigest.getInstance("SHA-256")
				.digest(secret.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("토큰 암호화 키 생성에 실패했습니다.", exception);
		}
	}
}

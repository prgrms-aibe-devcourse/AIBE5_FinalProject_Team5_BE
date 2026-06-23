package com.bootsignal.domain.user.storage;

import com.bootsignal.global.config.properties.AwsProperties;
import com.bootsignal.global.config.properties.ProfileImageProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ProfileImageStorage implements ProfileImageStorage {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/webp"
	);

	private final S3Client s3Client;
	private final AwsProperties awsProperties;
	private final ProfileImageProperties profileImageProperties;

	public S3ProfileImageStorage(
		S3Client s3Client,
		AwsProperties awsProperties,
		ProfileImageProperties profileImageProperties
	) {
		if (profileImageProperties.s3() == null
			|| !StringUtils.hasText(profileImageProperties.s3().baseUrl())
			|| !StringUtils.hasText(profileImageProperties.s3().keyPrefix())) {
			throw new IllegalStateException(
				"storage-type=s3 설정 시 PROFILE_IMAGE_S3_BASE_URL, PROFILE_IMAGE_S3_KEY_PREFIX 환경 변수가 필요합니다."
			);
		}
		this.s3Client = s3Client;
		this.awsProperties = awsProperties;
		this.profileImageProperties = profileImageProperties;
	}

	@Override
	public String store(MultipartFile file, Long userId) {
		validateFile(file);

		String extension = resolveExtension(file);
		String key = profileImageProperties.s3().keyPrefix() + "/" + userId + "_" + UUID.randomUUID() + extension;

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException exception) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 이미지 저장에 실패했습니다.");
		}

		try {
			s3Client.putObject(
				PutObjectRequest.builder()
					.bucket(awsProperties.s3().bucket())
					.key(key)
					.contentType(file.getContentType())
					.contentLength(file.getSize())
					.build(),
				RequestBody.fromBytes(bytes)
			);
		} catch (Exception exception) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 이미지 저장에 실패했습니다.");
		}

		String baseUrl = profileImageProperties.s3().baseUrl().replaceAll("/$", "");
		return baseUrl + "/" + key;
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "프로필 이미지 파일이 필요합니다.");
		}
		String contentType = file.getContentType();
		if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. (jpeg, png, webp)");
		}
		if (!hasValidImageSignature(file, contentType)) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "이미지 파일 내용이 형식과 일치하지 않습니다.");
		}
	}

	private String resolveExtension(MultipartFile file) {
		return switch (file.getContentType()) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};
	}

	private boolean hasValidImageSignature(MultipartFile file, String contentType) {
		try {
			byte[] bytes = file.getBytes();
			return switch (contentType) {
				case "image/jpeg" -> hasPrefix(bytes, 0xFF, 0xD8, 0xFF);
				case "image/png" -> hasPrefix(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
				case "image/webp" -> hasPrefix(bytes, 0x52, 0x49, 0x46, 0x46)
					&& hasBytesAt(bytes, 8, 0x57, 0x45, 0x42, 0x50);
				default -> false;
			};
		} catch (IOException exception) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "이미지 파일을 읽을 수 없습니다.");
		}
	}

	private boolean hasPrefix(byte[] bytes, int... expected) {
		return hasBytesAt(bytes, 0, expected);
	}

	private boolean hasBytesAt(byte[] bytes, int offset, int... expected) {
		if (bytes.length < offset + expected.length) {
			return false;
		}
		for (int index = 0; index < expected.length; index++) {
			if ((bytes[offset + index] & 0xFF) != expected[index]) {
				return false;
			}
		}
		return true;
	}
}

package com.bootsignal.domain.user.storage;

import com.bootsignal.global.config.properties.ProfileImageProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 디스크에 프로필 이미지를 저장하고 공개 URL을 생성하는 저장소입니다.
 * 업로드 파일은 허용된 이미지 형식과 파일 시그니처를 모두 통과해야 저장됩니다.
 */
public class LocalProfileImageStorage implements ProfileImageStorage {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/webp"
	);

	private final ProfileImageProperties profileImageProperties;

	public LocalProfileImageStorage(ProfileImageProperties profileImageProperties) {
		this.profileImageProperties = profileImageProperties;
	}

	@Override
	public String store(MultipartFile file, Long userId) {
		validateFile(file);

		String extension = resolveExtension(file);
		String filename = userId + "_" + UUID.randomUUID() + extension;
		Path uploadDir = resolveUploadDir();

		try {
			Files.createDirectories(uploadDir);
			Path target = uploadDir.resolve(filename).normalize();
			if (!target.startsWith(uploadDir)) {
				throw new BootSignalException(ErrorCode.BAD_REQUEST, "파일 저장 경로가 올바르지 않습니다.");
			}
			file.transferTo(target);
		} catch (IOException exception) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 이미지 저장에 실패했습니다.");
		}

		String publicPath = profileImageProperties.local().publicPathPrefix();
		if (!publicPath.startsWith("/")) {
			publicPath = "/" + publicPath;
		}
		if (publicPath.endsWith("/")) {
			publicPath = publicPath.substring(0, publicPath.length() - 1);
		}

		String baseUrl = profileImageProperties.local().baseUrl().replaceAll("/$", "");
		return baseUrl + publicPath + "/" + filename;
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

	private Path resolveUploadDir() {
		return Path.of(System.getProperty("user.dir"))
			.resolve(profileImageProperties.local().uploadDir())
			.normalize();
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

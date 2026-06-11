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

/** 루트/temp-storage/profile-images 프로필 이미지 임시 저장 **/
	// 로컬 저장: storage-type=local 일 때만 Bean으로 등록됨
	// S3 저장: storage-type=s3 설정시, 로컬 저장 자동 비활성화됨

public class LocalProfileImageStorage implements ProfileImageStorage {

	/** 허용 이미지 형식 **/
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/webp"
	);

	/** 프로필 이미지 저장 설정 **/
	private final ProfileImageProperties profileImageProperties;

	public LocalProfileImageStorage(ProfileImageProperties profileImageProperties) {
		this.profileImageProperties = profileImageProperties;
	}

	/** 프로필 이미지 저장 **/
	@Override
	public String store(MultipartFile file, Long userId) {
		// 파일 유효성 검사
		validateFile(file); 

		// 파일 경로 설정
		String extension = resolveExtension(file);
		String filename = userId + "_" + UUID.randomUUID() + extension; 
		Path uploadDir = resolveUploadDir();

		// 파일 저장
		try {
			Files.createDirectories(uploadDir);
			Path target = uploadDir.resolve(filename);
			file.transferTo(target);
		} catch (IOException exception) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 이미지 저장에 실패했습니다.");
		}

		// 공개 URL 생성 & 반환
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

	// 파일 유효성 검사
	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "프로필 이미지 파일이 필요합니다.");
		}
		String contentType = file.getContentType(); 
		if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new BootSignalException(ErrorCode.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. (jpeg, png, webp)");
		}
	}

	// 설정 파일 기반 업로드 디렉토리 결정 
	private Path resolveUploadDir() {
		// 프로젝트 루트 기준 업로드 디렉토리 결정
		return Path.of(System.getProperty("user.dir"))
			.resolve(profileImageProperties.local().uploadDir())
			.normalize();
	}

	// 파일 확장자 결정
	private String resolveExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
			return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
		}

		return switch (file.getContentType()) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};
	}
}

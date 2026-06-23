package com.bootsignal.global.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 프로필 이미지 저장 설정 application.yml에서 바인딩 **/
	// S3 전환 시 수정 사항:
		// 1. application.yml app.profile-image.s3 블록 추가
 		// 2. ProfileImageProperties 파라미터에 @Valid S3 s3 필드 추가
 		// 3. 아래 S3 record 구현

@Validated
@ConfigurationProperties(prefix = "app.profile-image")
public record ProfileImageProperties(
	@NotBlank String storageType,
	@Valid Local local,
	S3 s3
) {

	/** storage-type=local 일 때 사용 */
	public record Local(
		@NotBlank String uploadDir,
		@NotBlank String publicPathPrefix,
		@NotBlank String baseUrl
	) {
	}

	/** storage-type=s3 일 때 사용 */
	public record S3(
		String keyPrefix,
		String baseUrl
	) {
	}
}

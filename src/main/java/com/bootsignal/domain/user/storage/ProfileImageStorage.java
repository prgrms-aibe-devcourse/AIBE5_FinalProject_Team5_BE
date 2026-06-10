package com.bootsignal.domain.user.storage;

import org.springframework.web.multipart.MultipartFile;

/** 프로필 이미지 저장소 추상화 **/
	// 현재: LocalProfileImageStorage.java — 프로젝트 루트/temp-storage/profile-images

	// S3 전환 시: S3ProfileImageStorage.java - S3 업로드 
		// 1. S3ProfileImageStorage.java 구현
 		// 2. ProfileImageStorageConfig에 s3 Bean 등록
 		// 3. application.yml 파일 storage-type을 s3로 변경
	
public interface ProfileImageStorage {

	String store(MultipartFile file, Long userId);
}

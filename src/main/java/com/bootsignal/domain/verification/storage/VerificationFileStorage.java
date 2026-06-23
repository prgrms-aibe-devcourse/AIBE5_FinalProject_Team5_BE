package com.bootsignal.domain.verification.storage;

import org.springframework.web.multipart.MultipartFile;

/** 수강 인증 자료 파일 저장소 추상화 **/
public interface VerificationFileStorage {

	String upload(MultipartFile file, String keyPrefix);

	byte[] download(String s3Key);
}

package com.bootsignal.domain.verification.storage;

import com.bootsignal.global.config.properties.AwsProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3VerificationFileStorage implements VerificationFileStorage {

	private final S3Client s3Client;
	private final AwsProperties awsProperties;

	public S3VerificationFileStorage(S3Client s3Client, AwsProperties awsProperties) {
		this.s3Client = s3Client;
		this.awsProperties = awsProperties;
	}

	@Override
	public String upload(MultipartFile file, String keyPrefix) {
		String originalFilename = file.getOriginalFilename();
		String extension = resolveExtension(originalFilename);
		String key = keyPrefix + "/" + UUID.randomUUID() + extension;

		try {
			s3Client.putObject(
				PutObjectRequest.builder()
					.bucket(awsProperties.s3().bucket())
					.key(key)
					.contentType(resolveContentType(file))
					.contentLength(file.getSize())
					.build(),
				RequestBody.fromBytes(file.getBytes())
			);
		} catch (IOException exception) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "인증 자료 업로드에 실패했습니다.");
		}

		return key;
	}

	@Override
	public byte[] download(String s3Key) {
		try {
			ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
				GetObjectRequest.builder()
					.bucket(awsProperties.s3().bucket())
					.key(s3Key)
					.build()
			);
			return response.asByteArray();
		} catch (Exception exception) {
			throw new BootSignalException(ErrorCode.INTERNAL_SERVER_ERROR, "인증 자료 다운로드에 실패했습니다.");
		}
	}

	private String resolveExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf('.'));
	}

	private String resolveContentType(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null ? contentType : "application/octet-stream";
	}
}

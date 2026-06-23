package com.bootsignal.domain.user.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.bootsignal.global.config.properties.ProfileImageProperties;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("LocalProfileImageStorage 테스트")
class LocalProfileImageStorageTest {

	@TempDir
	Path tempDir;

	@Test
	@DisplayName("이미지 파일을 로컬 디렉터리에 저장하고 공개 URL을 반환한다")
	void storeSavesFileAndReturnsPublicUrl() throws IOException {
		// given
		LocalProfileImageStorage storage = new LocalProfileImageStorage(properties(tempDir));
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			pngBytes()
		);

		// when
		String profileImageUrl = storage.store(file, 1L);

		// then
		assertThat(profileImageUrl).startsWith("http://localhost:8080/local-files/profile/1_");
		assertThat(profileImageUrl).endsWith(".png");
		try (var files = Files.list(tempDir)) {
			assertThat(files).hasSize(1);
		}
	}

	@Test
	@DisplayName("빈 파일이면 BAD_REQUEST 예외를 던진다")
	void storeThrowsBadRequestWhenFileIsEmpty() {
		// given
		LocalProfileImageStorage storage = new LocalProfileImageStorage(properties(tempDir));
		MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

		// when & then
		assertThatThrownBy(() -> storage.store(file, 1L))
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BAD_REQUEST);
	}

	@Test
	@DisplayName("지원하지 않는 형식이면 BAD_REQUEST 예외를 던진다")
	void storeThrowsBadRequestWhenContentTypeIsUnsupported() {
		// given
		LocalProfileImageStorage storage = new LocalProfileImageStorage(properties(tempDir));
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.txt",
			"text/plain",
			"text".getBytes()
		);

		// when & then
		assertThatThrownBy(() -> storage.store(file, 1L))
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BAD_REQUEST);
	}

	@Test
	@DisplayName("원본 파일 확장자가 달라도 Content-Type 기준 확장자로 저장한다")
	void storeUsesContentTypeExtensionInsteadOfOriginalFilename() {
		// given
		LocalProfileImageStorage storage = new LocalProfileImageStorage(properties(tempDir));
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.html",
			"image/png",
			pngBytes()
		);

		// when
		String profileImageUrl = storage.store(file, 1L);

		// then
		assertThat(profileImageUrl).endsWith(".png");
	}

	@Test
	@DisplayName("Content-Type과 파일 내용이 일치하지 않으면 BAD_REQUEST 예외를 던진다")
	void storeThrowsBadRequestWhenImageSignatureIsInvalid() {
		// given
		LocalProfileImageStorage storage = new LocalProfileImageStorage(properties(tempDir));
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			"not-an-image".getBytes()
		);

		// when & then
		assertThatThrownBy(() -> storage.store(file, 1L))
			.isInstanceOf(BootSignalException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.BAD_REQUEST);
	}

	private ProfileImageProperties properties(Path uploadDir) {
		return new ProfileImageProperties(
			"local",
			new ProfileImageProperties.Local(
				uploadDir.toString(),
				"/local-files/profile",
				"http://localhost:8080"
			),
			null
		);
	}

	private byte[] pngBytes() {
		return new byte[] {
			(byte) 0x89, 0x50, 0x4E, 0x47,
			0x0D, 0x0A, 0x1A, 0x0A,
			0x00, 0x00, 0x00, 0x0D
		};
	}
}

package com.bootsignal.global.config;

import com.bootsignal.global.config.properties.ProfileImageProperties;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 웹으로 전달된 프로필 이미지 요청 URL을 실제 파일 경로와 매핑 **/
@Configuration
@EnableConfigurationProperties(ProfileImageProperties.class)
@ConditionalOnProperty(name = "app.profile-image.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalProfileImageWebConfig implements WebMvcConfigurer {

	// 프로필 이미지 저장 설정 셋팅
	private final ProfileImageProperties profileImageProperties;

	public LocalProfileImageWebConfig(ProfileImageProperties profileImageProperties) {
		this.profileImageProperties = profileImageProperties;
	}

	/** 프로필 이미지 저장 경로 셋팅 **/
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// 공개 URL 설정
		String publicPath = profileImageProperties.local().publicPathPrefix();
		if (!publicPath.startsWith("/")) {
			publicPath = "/" + publicPath;
		}
		if (!publicPath.endsWith("/")) {
			publicPath = publicPath + "/";
		}

		// 업로드 경로 설정
		Path uploadPath = Path.of(System.getProperty("user.dir"))
			.resolve(profileImageProperties.local().uploadDir())
			.normalize();
		String uploadDir = uploadPath.toUri().toString();
		if (!uploadDir.endsWith("/")) {
			uploadDir = uploadDir + "/";
		}

		// 공개 URL 요청을 디스크 저장 경로의 파일과 매핑
		registry.addResourceHandler(publicPath + "**")
			.addResourceLocations(uploadDir);
	}
}

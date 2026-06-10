package com.bootsignal.global.config;

import com.bootsignal.domain.user.storage.LocalProfileImageStorage;
import com.bootsignal.domain.user.storage.ProfileImageStorage;
import com.bootsignal.global.config.properties.ProfileImageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** ProfileImageStorage 구현체 Bean 등록 **/
	// (application.yml) app.profile-image.storage-type 설정 값에 따라 
	// local 또는 s3 Bean 중 하나만 활성화됨
@Configuration
public class ProfileImageStorageConfig {

	@Bean
	@ConditionalOnProperty(name = "app.profile-image.storage-type", havingValue = "local", matchIfMissing = true)
	public ProfileImageStorage localProfileImageStorage(ProfileImageProperties profileImageProperties) {
		return new LocalProfileImageStorage(profileImageProperties);
	}

	// S3 전환 시: 아래  storage-type=s3 Bean 설정
	// @ConditionalOnProperty(name = "app.profile-image.storage-type", havingValue = "s3")
	// public ProfileImageStorage s3ProfileImageStorage() {}
}

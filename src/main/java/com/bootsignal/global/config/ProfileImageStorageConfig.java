package com.bootsignal.global.config;

import com.bootsignal.domain.user.storage.LocalProfileImageStorage;
import com.bootsignal.domain.user.storage.ProfileImageStorage;
import com.bootsignal.domain.user.storage.S3ProfileImageStorage;
import com.bootsignal.global.config.properties.AwsProperties;
import com.bootsignal.global.config.properties.ProfileImageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

/** ProfileImageStorage 구현체 Bean 등록 **/
	// (application.yml) app.profile-image.storage-type 설정 값에 따라
	// local 또는 s3 Bean 중 하나만 활성화됨
@Configuration
@EnableConfigurationProperties(ProfileImageProperties.class)
public class ProfileImageStorageConfig {

	@Bean
	@ConditionalOnProperty(name = "app.profile-image.storage-type", havingValue = "local", matchIfMissing = true)
	public ProfileImageStorage localProfileImageStorage(ProfileImageProperties profileImageProperties) {
		return new LocalProfileImageStorage(profileImageProperties);
	}

	@Bean
	@ConditionalOnProperty(name = "app.profile-image.storage-type", havingValue = "s3")
	public ProfileImageStorage s3ProfileImageStorage(
		S3Client s3Client,
		AwsProperties awsProperties,
		ProfileImageProperties profileImageProperties
	) {
		return new S3ProfileImageStorage(s3Client, awsProperties, profileImageProperties);
	}
}

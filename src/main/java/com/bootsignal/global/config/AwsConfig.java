package com.bootsignal.global.config;

import com.bootsignal.global.config.properties.AwsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

	@Bean
	public S3Client s3Client(AwsProperties awsProperties) {
		return S3Client.builder()
			.region(Region.of(awsProperties.region()))
			.credentialsProvider(DefaultCredentialsProvider.builder().build())
			.build();
	}
}

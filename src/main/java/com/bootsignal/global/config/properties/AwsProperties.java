package com.bootsignal.global.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(
	@NotBlank String region,
	@Valid S3 s3
) {

	public record S3(
		@NotBlank String bucket
	) {
	}
}


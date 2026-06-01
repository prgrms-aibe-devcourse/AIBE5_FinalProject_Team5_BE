package com.bootsignal.global.health.dto;

public record HealthCheckResponse(
	String status,
	String message
) {
}

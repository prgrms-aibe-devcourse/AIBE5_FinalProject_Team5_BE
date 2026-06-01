package com.bootsignal.global.health.controller;

import com.bootsignal.global.health.dto.HealthCheckResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

	@GetMapping
	public HealthCheckResponse healthCheck() {
		// 서버 상태 확인 응답입니다.
		return new HealthCheckResponse("UP", "BootSignal API is running.");
	}
}

package com.bootsignal.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ValidationTestController {

	@PostMapping("/test/validate")
	ValidationResponse validate(@Valid @RequestBody ValidationRequest request) {
		return new ValidationResponse(request.name());
	}

	record ValidationRequest(
		@NotBlank(message = "이름은 필수입니다.")
		String name
	) {
	}

	record ValidationResponse(
		String name
	) {
	}
}

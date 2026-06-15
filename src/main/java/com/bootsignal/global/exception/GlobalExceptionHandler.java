package com.bootsignal.global.exception;

import com.bootsignal.global.response.ApiResponse;
import com.bootsignal.global.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BootSignalException.class)
	public ResponseEntity<ApiResponse<Void>> handleBootSignalException(BootSignalException exception) {
		ErrorCode errorCode = exception.errorCode();
		return toResponse(errorCode, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception
	) {
		return toResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.message(), fieldErrors(exception));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
		return toResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.message(), fieldErrors(exception));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
		ConstraintViolationException exception
	) {
		// 메서드 파라미터 검증 실패를 필드 단위 오류로 변환한다.
		List<ErrorResponse.FieldError> fieldErrors = exception.getConstraintViolations().stream()
			.map(violation -> new ErrorResponse.FieldError(
				violation.getPropertyPath().toString(),
				violation.getMessage()
			))
			.toList();
		return toResponse(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.message(), fieldErrors);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
		MissingServletRequestParameterException exception
	) {
		String message = String.format("필수 요청 파라미터 '%s'가 없습니다.", exception.getParameterName());
		return toResponse(ErrorCode.BAD_REQUEST, message);
	}

	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class,
		IllegalArgumentException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception exception) {
		return toResponse(ErrorCode.BAD_REQUEST, resolveMessage(exception, ErrorCode.BAD_REQUEST.message()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
		return toResponse(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.message());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
		HttpRequestMethodNotSupportedException exception
	) {
		return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.message());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(
		HttpMediaTypeNotSupportedException exception
	) {
		return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.message());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
		// @PreAuthorize 등 메서드 보안 인가 실패(AuthorizationDeniedException 포함)는 403으로 응답한다.
		// 핸들러가 없으면 catch-all(Exception)이 가로채 500으로 내려가므로 명시적으로 처리한다.
		return toResponse(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.message());
	}

	@ExceptionHandler(IOException.class)
	public ResponseEntity<ApiResponse<Void>> handleIOException(IOException exception) {
		// 내부 상세 경로나 시스템 메시지가 응답에 노출되지 않도록 고정 메시지를 사용한다.
		log.warn("I/O exception occurred", exception);
		return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, "입출력 처리 중 오류가 발생했습니다.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		log.error("Unexpected exception occurred", exception);
		return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.message());
	}

	private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, String message) {
		return toResponse(errorCode, message, List.of());
	}

	private ResponseEntity<ApiResponse<Void>> toResponse(
		ErrorCode errorCode,
		String message,
		List<ErrorResponse.FieldError> fieldErrors
	) {
		ErrorResponse errorResponse = ErrorResponse.of(errorCode, message, fieldErrors);
		return ResponseEntity.status(errorCode.status()).body(ApiResponse.failure(errorResponse));
	}

	private List<ErrorResponse.FieldError> fieldErrors(BindException exception) {
		return exception.getBindingResult().getFieldErrors().stream()
			.map(this::toFieldError)
			.toList();
	}

	private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
		return new ErrorResponse.FieldError(fieldError.getField(), resolveMessage(fieldError));
	}

	private String resolveMessage(ObjectError objectError) {
		return resolveMessage(objectError.getDefaultMessage(), "유효하지 않은 값입니다.");
	}

	private String resolveMessage(Exception exception, String defaultMessage) {
		return resolveMessage(exception.getMessage(), defaultMessage);
	}

	private String resolveMessage(String message, String defaultMessage) {
		return message == null || message.isBlank() ? defaultMessage : message;
	}
}

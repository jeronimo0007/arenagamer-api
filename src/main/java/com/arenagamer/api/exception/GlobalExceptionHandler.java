package com.arenagamer.api.exception;

import com.arenagamer.api.dto.response.ApiErrorResponse;
import com.arenagamer.api.dto.response.ErrorCode;
import com.arenagamer.api.dto.response.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode code = mapStatus(ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorResponse.of(code, ex.getMessage(), ex.getStatus()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        String message = detail != null && detail.contains("uk_tournament_client_roster")
                ? "Um ou mais jogadores escalados já participam deste torneio por outra equipe"
                : "Operação conflita com dados existentes";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(ErrorCode.CONFLICT, message, HttpStatus.CONFLICT));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorDetail)
                .toList();

        String message = errors.stream()
                .map(ErrorDetail::getMessage)
                .reduce((a, b) -> a + ", " + b)
                .orElse(ErrorCode.VALIDATION_ERROR.getDefaultMessage());

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(ErrorCode.BAD_REQUEST, "Corpo da requisição JSON inválido", HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiErrorResponse.of(ErrorCode.BAD_REQUEST,
                        "Método HTTP não suportado para este endpoint: " + ex.getMethod(),
                        HttpStatus.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        ErrorCode.ENDPOINT_NOT_FOUND,
                        "Endpoint não encontrado. Verifique a URL (ex: /api/v1/public/auth/login, /api/v1/common/users/me)",
                        HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        log.error("Erro interno", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return ErrorDetail.of(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ErrorCode mapStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case CONFLICT -> ErrorCode.CONFLICT;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}

package com.arenagamer.api.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiResponses {

    private ApiResponses() {}

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(ApiMessages.SUCCESS, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> listed(T data) {
        return ResponseEntity.ok(ApiResponse.ok(ApiMessages.LIST_SUCCESS, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> fetched(T data) {
        return ResponseEntity.ok(ApiResponse.ok(ApiMessages.FETCH_SUCCESS, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }

    public static ResponseEntity<ApiResponse<Void>> okMessage(String message) {
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ApiMessages.CREATE_SUCCESS, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(message, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> updated(T data) {
        return ResponseEntity.ok(ApiResponse.ok(ApiMessages.UPDATE_SUCCESS, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> updated(String message, T data) {
        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }

    public static ResponseEntity<ApiResponse<Void>> deleted() {
        return ResponseEntity.ok(ApiResponse.ok(ApiMessages.DELETE_SUCCESS));
    }

    public static ResponseEntity<ApiResponse<Void>> deleted(String message) {
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}

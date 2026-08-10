package com.learning.order_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String timestamp;
    private int status;
    private String message;
    private T data;
    private List<String> errors;

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .message(message)
                .errors(errors)
                .build();
    }
}
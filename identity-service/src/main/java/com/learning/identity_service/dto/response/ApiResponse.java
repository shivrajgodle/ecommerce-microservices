package com.learning.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Every endpoint in this service — success or failure — returns THIS
 * shape. A frontend client only ever needs to learn one response
 * contract, not a different JSON shape per endpoint or per error type.
 *
 * @JsonInclude(NON_NULL) means 'errors' is omitted entirely from
 * successful responses instead of serializing as "errors": null —
 * keeps success payloads clean.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String timestamp;
    private int status;
    private String message;
    private T data;
    private List<String> errors;

    public static<T> ApiResponse<T> success(int status, String message, T data){
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .message(message)
                .data(data)
                .build();
    }


    public static<T> ApiResponse<T> error(int status, String message, List<String> errors){
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .message(message)
                .errors(errors)
                .build();
    }



}

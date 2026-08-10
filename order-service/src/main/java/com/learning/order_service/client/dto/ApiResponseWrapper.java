package com.learning.order_service.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApiResponseWrapper<T> {
    private String timestamp;
    private int status;
    private String message;
    private T data;
    private List<String> errors;
}

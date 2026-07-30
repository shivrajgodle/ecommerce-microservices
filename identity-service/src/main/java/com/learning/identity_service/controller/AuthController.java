package com.learning.identity_service.controller;

import com.learning.identity_service.dto.request.LoginRequest;
import com.learning.identity_service.dto.request.RefreshRequest;
import com.learning.identity_service.dto.request.RegisterRequest;
import com.learning.identity_service.dto.response.ApiResponse;
import com.learning.identity_service.dto.response.AuthResponse;
import com.learning.identity_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request){
        AuthResponse result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(),"Registration successful",result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request){
        AuthResponse result = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),"Login successful", result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshRequest request){
        AuthResponse result = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),"Token refreshed", result));
    }
}

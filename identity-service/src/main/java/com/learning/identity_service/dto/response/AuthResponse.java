package com.learning.identity_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType; // always "Bearer" — but explicit in the payload avoids client-side guessing
    private String expiresInMs;
}

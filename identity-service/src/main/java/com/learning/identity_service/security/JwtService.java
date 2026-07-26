package com.learning.identity_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtService {

    // @Value pulls a single scalar property from the environment (which,
    // remember, was assembled from Config Server + Eureka discovery back
    // in File 1). This is field injection of a VALUE, not a bean — a
    // different mechanism from constructor-injecting a dependency, worth
    // distinguishing clearly in your own head.
    @Value("${app.jwt.secret}")
    private String secretKeyBase64;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey(){
        // The secret is stored Base64-encoded in config (common practice —
        // keeps arbitrary binary key material safe inside a YAML string).
        // We decode it back to raw bytes here, then wrap it as a proper
        // HMAC-SHA key object the JJWT library understands.
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a short-lived ACCESS token. This is what gets sent as
     * "Authorization: Bearer <token>" on every subsequent API call.
     *
     * We embed roles as a custom claim so downstream authorization
     * checks (method security, filter checks) don't need a database
     * lookup at all — the token itself carries everything needed to
     * authorize the request. This is the performance property mentioned
     * in the previous file.
     */
    public String generateAccessToken(UserPrincipal userPrincipal){
        HashMap<String,Object> claims = new HashMap<>();
        claims.put("roles",userPrincipal.getAuthorities().stream().map(Object::toString).toList());
        claims.put("userId",userPrincipal.getUser().getId());
        return buildToken(claims,userPrincipal.getUsername(),accessTokenExpirationMs);
    }

    /**
     * Refresh tokens carry NO roles/claims beyond identity — deliberately
     * minimal. Their only job is "prove you were legitimately logged in
     * recently enough to be issued a new access token", not to authorize
     * API calls directly (the refresh endpoint should reject a refresh
     * token presented anywhere except the refresh endpoint itself).
     */
    public String generateRefreshToken(UserPrincipal userPrincipal){
        return buildToken(new HashMap<>(),userPrincipal.getUsername(),refreshTokenExpirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .claims(claims)
                .subject(subject) // WHO this token represents — the user's email
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey()) // this is what makes the token TAMPER-PROOF, not secret
                .compact();
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims,T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // parseSignedClaims throws if the signature doesn't match — this
        // single call is what cryptographically proves the token wasn't
        // tampered with since WE signed it.
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates a token against a specific expected user AND checks
     * expiry. Split into its own method (rather than folding into the
     * filter directly) so it's independently unit-testable.
     */
    public boolean isTokenValid(String token, UserDetails userDetails){
        try{
            String userName = extractUsername(token);
            Date expiration = extractClaim(token, Claims::getExpiration);
            return userName.equals(userDetails.getUsername()) && expiration.after(new Date());
        } catch(ExpiredJwtException | MalformedJwtException | SignatureException e){
            log.warn("JWT Validation Failed: {}",e.getMessage());
            return false;
        }
    }



}

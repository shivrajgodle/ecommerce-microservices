package com.learning.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Component
public class JwtValidationFilter implements GlobalFilter , Ordered {

    @Value("{app.jwt.secret}")
    private String secretKeyBase64;

    // Paths that must remain reachable WITHOUT a token — you can't be
    // asked to present a JWT in order to obtain your first JWT.
    private static final List<String> PUBLIC_PATHS = Arrays.asList("/api/v1/auth");


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if(PUBLIC_PATHS.stream().anyMatch(path::startsWith)){
            return chain.filter(exchange);
        }

        List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if(authHeaders == null || authHeaders.isEmpty() || !authHeaders.get(0).startsWith("Bearer ")){
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeaders.get(0).substring(7);

        try{
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            Object userId = claims.get("userId");

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            /**
             * ServerHttpRequest/ServerWebExchange are IMMUTABLE in the
             * reactive stack — there's no request.setHeader(...) like
             * the Servlet world's HttpServletRequest. Instead, you build
             * a MODIFIED COPY via .mutate(), then a modified copy of the
             * exchange wrapping that new request, and pass THAT forward
             * in the chain. This immutability is deliberate — it makes
             * the reactive pipeline safe to process concurrently without
             * shared mutable state between requests.
             */
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Auth-User-Email",email)
                    .header("X-Auth-User-Id",String.valueOf(userId))
                    .header("X-Auth-User-Roles", String.join(",",roles))
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch(ExpiredJwtException | MalformedJwtException | SignatureException e){
            return unauthorized(exchange,"Invalid or expired JWT");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
         ServerHttpResponse response = exchange.getResponse();
         response.setStatusCode(HttpStatus.UNAUTHORIZED);
         response.getHeaders().add("Content-Type","application/json");

         String body = String.format("{\"timestamp\":\"%s\",\"status\":401,\"message\":\"%s\"}", LocalDateTime.now(),message);

         DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

         return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Lower values run earlier. We want token validation to happen
        // BEFORE Gateway's own routing/proxying filter, which by default
        // runs at Ordered.LOWEST_PRECEDENCE (i.e., last).
        return -1;
    }
}

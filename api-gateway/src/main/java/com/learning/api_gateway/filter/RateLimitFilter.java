package com.learning.api_gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /**
     * ⚠️ IN-MEMORY, PER-GATEWAY-INSTANCE state — flagging this
     * honestly up front, explained fully below. Fine for local
     * learning with one Gateway instance; a real production deployment
     * running multiple Gateway instances behind a load balancer would
     * need this to be SHARED state (Redis-backed), or each instance
     * silently enforces its own independent limit, multiplying the
     * effective rate by however many instances are running.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${app.ratelimit.public.capacity}")
    private int publicCapacity;
    @Value("${app.ratelimit.public.refill-tokens}")
    private int publicRefillTokens;
    @Value("${app.ratelimit.public.refill-period-seconds}")
    private int publicRefillPeriodSeconds;

    @Value("${app.ratelimit.authenticated.capacity}")
    private int authCapacity;
    @Value("${app.ratelimit.authenticated.refill-tokens}")
    private int authRefillTokens;
    @Value("${app.ratelimit.authenticated.refill-period-seconds}")
    private int authRefillPeriodSeconds;

    private static final List<String> PUBLIC_PATHS = List.of("/api/v1/auth");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);

        String bucketKey;
        Bucket bucket;

        if(isPublic){
            // No identity available yet at this point for public
            // endpoints — key by IP instead. This is exactly the
            // "brute-force login" scenario this stricter limit exists for.
            String clientIp = getClientIp(exchange);
            bucketKey = "public:" + clientIp;
            bucket = buckets.computeIfAbsent(bucketKey,
                    k -> newBucket(publicCapacity, publicRefillTokens, publicRefillPeriodSeconds));
        } else{
            // For every OTHER path, JwtValidationFilter (Phase D, File
            // 2) has ALREADY run by the time we get here — see
            // getOrder() below — so X-Auth-User-Id is guaranteed
            // present for any request that reached this point. Keying
            // by actual user identity, rather than IP, means each
            // logged-in user gets their own fair allowance regardless
            // of shared IPs (offices, mobile carriers behind NAT, etc.).
            String userId = exchange.getRequest().getHeaders().getFirst("X-Auth-User-Id");
            bucketKey = "user:" + userId;
            bucket = buckets.computeIfAbsent(bucketKey, k -> newBucket(authCapacity, authRefillTokens, authRefillPeriodSeconds));
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if(probe.isConsumed()){
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",String.valueOf(probe.getRemainingTokens()));
            return chain.filter(exchange);
        }
        return tooManyRequests(exchange,probe);
    }

    /**
     * Bucket4j's TOKEN BUCKET algorithm: the bucket starts full at
     * 'capacity' tokens. Every request consumes one token. Tokens
     * refill at a steady rate ('refillTokens' every 'refillPeriod').
     * This allows short BURSTS up to the full capacity, while still
     * enforcing the average rate over time — genuinely different from
     * a naive fixed-window counter (explained in full below).
     */
    private Bucket newBucket(int capacity, int refillTokens, int refillPeriodSeconds) {
     Bandwidth limit = Bandwidth.classic(capacity,
             Refill.intervally(refillTokens, Duration.ofSeconds(refillPeriodSeconds)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, ConsumptionProbe probe) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE,"application/json");

        // Tells the CLIENT exactly when it's worth retrying, computed
        // directly from Bucket4j's own refill math — not a guess.
        long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        response.getHeaders().add(HttpHeaders.RETRY_AFTER,String.valueOf(waitSeconds));

        String body = String.format(
                "{\"status\":429,\"message\":\"Rate limit exceeded. Retry after %d seconds.\"}", waitSeconds);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String getClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    /**
     * Order 0 — runs AFTER JwtValidationFilter's order -1. This is a
     * deliberate dependency between the two filters: rate limiting
     * needs X-Auth-User-Id to already be on the request for the
     * authenticated-path branch above, which only exists once JWT
     * validation has already added it.
     */
    @Override
    public int getOrder() {
        return 0;
    }
}

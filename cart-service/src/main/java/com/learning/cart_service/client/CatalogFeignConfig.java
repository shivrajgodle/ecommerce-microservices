package com.learning.cart_service.client;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * DELIBERATELY not annotated @Configuration, and this class must live
 * where the main application's component scan won't pick it up as a
 * global bean (it's referenced explicitly per-client below instead).
 * Feign gives each @FeignClient its own isolated Spring child context
 * specifically so client-specific configuration like this doesn't leak
 * out and silently override beans for OTHER Feign clients (or the main
 * app) elsewhere in the service. Getting this wrong — accidentally
 * making a Feign config class globally component-scanned — is a
 * genuinely common, confusing bug once a service has more than one
 * Feign client with different needs.
 */
public class CatalogFeignConfig {

    @Bean
    public ErrorDecoder errorDecoder(){
        return new CatalogServiceErrorDecoder();
    }
}

package com.learning.catalog_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @EnableCaching switches on Spring's caching abstraction — without it,
 * @Cacheable/@CacheEvict annotations we add later are completely inert,
 * exactly like @EnableJpaAuditing was for @CreatedDate back in Phase 1.
 * Same underlying pattern: annotations are just metadata until something
 * explicitly turns on the infrastructure that interprets them.
 */
@SpringBootApplication
@EnableCaching
@EnableRetry
public class CatalogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

}

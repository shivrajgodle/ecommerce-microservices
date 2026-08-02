package com.learning.cart_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @EnableFeignClients scans this application's packages for interfaces
 * annotated @FeignClient (we write one in File 3) and generates a
 * runtime proxy implementation for each — the same "interface in, proxy
 * implementation out" pattern Spring Data JPA repositories use, just
 * applied to outbound HTTP calls instead of database queries.
 */
@SpringBootApplication
@EnableFeignClients
public class CartServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartServiceApplication.class, args);
	}

}

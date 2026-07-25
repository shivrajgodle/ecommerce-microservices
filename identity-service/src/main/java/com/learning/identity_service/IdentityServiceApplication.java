package com.learning.identity_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Same thin-entry-point philosophy as the monolith's main class.
 * Having spring-cloud-starter-netflix-eureka-client on the classpath is
 * enough for this service to auto-register with Eureka on startup —
 * no annotation needed (older Spring Cloud required @EnableDiscoveryClient
 * explicitly; modern versions auto-detect the client on the classpath).
 */
@SpringBootApplication
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}

}

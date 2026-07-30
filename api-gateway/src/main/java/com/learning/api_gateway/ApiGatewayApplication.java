package com.learning.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * No special annotation needed here — having spring-cloud-starter-gateway
 * plus the Eureka/LoadBalancer starters on the classpath is enough for
 * Spring Boot's auto-configuration to wire up the reactive routing
 * engine, service discovery integration, and load-balanced request
 * resolution automatically.
 */
@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}

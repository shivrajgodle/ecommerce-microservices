package com.learning.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @EnableEurekaServer turns this ordinary Spring Boot app into a Eureka
 * registry server — it stands up the registry data structure, the REST
 * API other services call to register/renew/deregister, and a small
 * dashboard UI at http://localhost:8761.
 *
 * That's the entire application. Nothing else needs to be written here —
 * the starter + this one annotation is the whole discovery server.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServerApplication.class, args);
	}

}

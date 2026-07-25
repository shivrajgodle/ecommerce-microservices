package com.learning.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;


/**
 * @EnableConfigServer turns this app into a config-serving REST API.
 * Clients hit endpoints like GET /identity-service/default to retrieve
 * that service's resolved configuration as JSON/YAML.
 *
 * Note: no @EnableDiscoveryClient needed explicitly — having the Eureka
 * client starter on the classpath is enough for Spring Cloud to
 * auto-register this app with Eureka. The annotation exists for older
 * Spring Cloud versions / explicitness, but modern Spring Cloud
 * auto-detects it. Worth knowing both ways exist.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}

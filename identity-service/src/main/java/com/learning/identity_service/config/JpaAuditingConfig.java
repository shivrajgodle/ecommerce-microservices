package com.learning.identity_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA's auditing infrastructure.
 *
 * Without this annotation, @CreatedDate / @LastModifiedDate fields
 * in BaseEntity would just sit there as plain columns — Spring only
 * populates them automatically once auditing is explicitly enabled.
 *
 * This is a separate, tiny @Configuration class (rather than putting
 * @EnableJpaAuditing on the main application class) so that later,
 * when we add @CreatedBy/@LastModifiedBy (who made the change, not just
 * when), we have one clear place to also wire an AuditorAware bean —
 * keeps auditing concerns isolated from the app's bootstrap class.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

}

package com.learning.identity_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Kept as its own small config class (rather than dumped into the bigger
 * SecurityConfig we'll write in File 3c) because this bean is also
 * needed by CustomUserDetailsService below via constructor injection,
 * and keeping bean definitions decoupled from the filter-chain config
 * avoids a circular-dependency headache later.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * BCrypt is a deliberately SLOW, adaptive hashing algorithm — slowness
     * is a FEATURE here, not a bug. It makes brute-force password
     * cracking computationally expensive even if your password database
     * leaks. It also automatically generates and stores a random salt
     * per password (embedded in the hash output itself), so two users
     * with the identical password get completely different hash strings.
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}

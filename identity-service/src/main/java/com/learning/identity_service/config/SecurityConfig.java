package com.learning.identity_service.config;

import com.learning.identity_service.security.CustomAuthenticationEntryPoint;
import com.learning.identity_service.security.CustomUserDetailsService;
import com.learning.identity_service.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection exists to defend browser-based, cookie-
                // session-authenticated apps against forged cross-site form
                // submissions. We authenticate via a Bearer token in a
                // header, not a cookie — there's no session for an attacker
                // to ride on, so CSRF simply doesn't apply here. Disabling
                // it isn't "turning off security", it's removing a
                // protection that has nothing to defend against in this
                // architecture.

                .csrf(AbstractHttpConfigurer::disable)
                
                // STATELESS is the single most important line in this class:
                // it tells Spring Security to NEVER create or read an
                // HttpSession. Every request must carry its own proof of
                // identity (the JWT) — nothing is remembered server-side
                // between requests. This is what makes this service
                // horizontally scalable with zero session-affinity/sticky-
                // session concerns.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                                auth.requestMatchers("/api/v1/auth/**").permitAll()
                                        .requestMatchers("/actuator/health","/actuator/info").permitAll()
                                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint))
                .authenticationProvider(authenticationProvider())
                // Placing our filter BEFORE Spring's built-in
                // UsernamePasswordAuthenticationFilter matters: it means our
                // JWT-based authentication happens first in the chain, and
                // if it successfully authenticates the request, later
                // filters simply see an already-authenticated context and
                // don't attempt their own (form-login-oriented) processing.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * The AuthenticationProvider is what actually KNOWS HOW to verify
     * credentials — DaoAuthenticationProvider specifically does it by
     * loading a UserDetails via our CustomUserDetailsService, then
     * comparing the submitted raw password against the stored hash
     * using our BCryptPasswordEncoder. This bean plugs both of those
     * pieces (3a's work) into Security's core authentication mechanism.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * AuthenticationManager is the entry point OUR CODE calls (from
     * AuthService, next file) to actually trigger authentication — e.g.
     * during login. It internally delegates to the AuthenticationProvider(s)
     * registered above. We don't build it ourselves; Spring already
     * assembles one from the AuthenticationConfiguration — we just expose
     * it as a bean so it's injectable.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }

}

package com.learning.identity_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * OncePerRequestFilter guarantees this runs exactly once per request,
 * even across internal forwards/includes (a plain jakarta.servlet.Filter
 * doesn't guarantee that). This is where a raw "Authorization: Bearer
 * <token>" header becomes a fully authenticated SecurityContext that the
 * rest of the request pipeline (controllers, method security) can trust.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token, or malformed header — let the request continue
        // unauthenticated. We do NOT reject here; whether that's okay
        // is decided later by the SecurityFilterChain's authorization
        // rules (e.g. /api/v1/auth/** is intentionally public).
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extractUsername(jwt);

        // The null check on existing Authentication matters: it prevents
        // this filter from redundantly re-authenticating a request that
        // some OTHER mechanism already authenticated earlier in the
        // chain — not a scenario we have yet, but a correct defensive
        // habit for any filter that writes to the SecurityContext.
        if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            if(jwtService.isTokenValid(jwt,userDetails)){
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, // credentials — null because we're not re-checking a password here, the JWT signature already proved identity
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // THIS is the line that actually authenticates the
                // request for everything downstream — controllers,
                // @PreAuthorize checks, etc. all read from here.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request,response);
    }
}

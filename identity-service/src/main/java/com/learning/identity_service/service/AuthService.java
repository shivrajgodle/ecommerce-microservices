package com.learning.identity_service.service;

import com.learning.identity_service.dto.request.LoginRequest;
import com.learning.identity_service.dto.request.RefreshRequest;
import com.learning.identity_service.dto.request.RegisterRequest;
import com.learning.identity_service.dto.response.AuthResponse;
import com.learning.identity_service.entity.Role;
import com.learning.identity_service.entity.RoleName;
import com.learning.identity_service.entity.User;
import com.learning.identity_service.exception.DuplicateResourceException;
import com.learning.identity_service.repository.RoleRepository;
import com.learning.identity_service.repository.UserRepository;
import com.learning.identity_service.security.CustomUserDetailsService;
import com.learning.identity_service.security.JwtService;
import com.learning.identity_service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMS;

    @Transactional
    public AuthResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new DuplicateResourceException("An account with "+request.getEmail() + "already exists");
        }

        User user = new User(request.getFullName(),request.getEmail(),passwordEncoder.encode(request.getPassword())); // NEVER store the raw password

        // Every new signup defaults to ROLE_CUSTOMER. Promoting to
        // ROLE_SELLER/ROLE_ADMIN is a deliberate separate action (an
        // admin-only endpoint) — never something a registration request
        // itself should be able to grant to itself.

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow(() -> new IllegalStateException(
                "ROLE_CUSTOMER not seeded in database — check startup data"
        ));

        user.addRole(customerRole);
        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        return buildAuthResponse(principal);
    }


    public AuthResponse login(LoginRequest request){
        // This single call is where DaoAuthenticationProvider actually
        // does its work: loads the user via CustomUserDetailsService,
        // compares the raw password against the stored BCrypt hash.
        // It throws BadCredentialsException on failure — which,
        // again, currently surfaces as a default error until we wire
        // up global exception handling.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        return buildAuthResponse((UserPrincipal) userDetails);
    }

    public AuthResponse refresh(RefreshRequest request){
        String email = jwtService.extractUsername(request.getRefreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if(!jwtService.isTokenValid(request.getRefreshToken(), userDetails)){
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }

        // Note: we're issuing a NEW access token but reusing the SAME
        // refresh token here for simplicity. Production systems commonly
        // rotate the refresh token too on every use (issue a new one,
        // invalidate the old) — this narrows the window an attacker who
        // steals one refresh token can exploit it, at the cost of needing
        // server-side tracking of which refresh tokens are still valid,
        // which reintroduces some statefulness. Worth knowing this
        // trade-off exists even though we're keeping it simple here.
        return buildAuthResponse((UserPrincipal) userDetails, request.getRefreshToken());
    }


    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        String refreshToken = jwtService.generateRefreshToken(principal);
        return buildAuthResponse(principal,refreshToken);
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(principal);
        return new AuthResponse(accessToken, refreshToken, "Bearer", String.valueOf(accessTokenExpirationMS));
    }

}

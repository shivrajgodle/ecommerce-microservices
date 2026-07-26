package com.learning.identity_service.security;


import com.learning.identity_service.entity.User;
import com.learning.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security calls loadUserByUsername() during authentication —
 * we're the ones telling it HOW to find a user (from our own database,
 * by email), which is exactly why this interface exists as a plug-in
 * point: Security doesn't care if your users come from Postgres, LDAP,
 * an external API, anywhere — it just needs something implementing
 * this one method.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new UsernameNotFoundException("No user not found with email:" + email));
        return new UserPrincipal(user);
    }
}

package com.learning.identity_service.security;

import com.learning.identity_service.entity.Role;
import com.learning.identity_service.entity.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security's entire authentication/authorization machinery works
 * against the UserDetails CONTRACT, not against our own User entity
 * directly. This adapter class implements that contract by wrapping a
 * real User — this keeps our JPA entity completely free of any Spring
 * Security-specific code (a clean separation: entity is genuinely just
 * a persistence concern, this class is genuinely just a security concern).
 */
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user){
        this.user = user;
    }

    /**
     * Converts our Role entities into Spring Security's GrantedAuthority
     * contract. This is exactly why we prefixed the enum values with
     * ROLE_ back in Phase 1 — Spring Security's hasRole("ADMIN") checks
     * internally look for an authority literally named "ROLE_ADMIN".
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<Role> roles = user.getRoles();
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName().name())).collect(Collectors.toSet());
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword(); // the BCrypt HASH, never plaintext
    }

    @Override
    public String getUsername() {
        // We're using email as the login identifier throughout this
        // service — "username" here is Spring Security's generic term
        // for "whatever uniquely identifies this principal", not
        // literally a username field.
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    /**
     * Escape hatch back to the real entity — controllers can call
     * @AuthenticationPrincipal UserPrincipal principal, then
     * principal.getUser() to get the actual User with its real ID,
     * full name, etc., rather than being stuck with just the
     * UserDetails contract's limited surface.
     */
    public User getUser() {
        return user;
    }
}

package com.example.evshare.security;

import com.example.evshare.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String password;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String fullName, String password, boolean isActive, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.isActive = isActive;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles() != null
                ? user.getRoles().stream()
                .filter(r -> r != null && r.getName() != null)
                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getIsActive()),
                authorities
        );
    }

    public static UserPrincipal fromClaims(Long id, String email, List<String> roles) {
        List<GrantedAuthority> authorities = roles != null
                ? roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new UserPrincipal(
                id,
                email,
                null,
                null,
                true,
                authorities
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}

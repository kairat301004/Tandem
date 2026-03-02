package org.example.tandem.security;

import org.example.tandem.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // можно расширить позже
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // можно расширить позже
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // можно расширить позже
    }

    @Override
    public boolean isEnabled() {
        return true; // можно привязать к user.status
    }

    public User getUser() {
        return user;
    }

    public static CustomUserDetails fromUser(User user) {
        return new CustomUserDetails(user);
    }
}
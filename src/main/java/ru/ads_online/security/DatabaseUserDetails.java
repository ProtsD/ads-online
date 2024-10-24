package ru.ads_online.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ru.ads_online.pojo.dto.user.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Component
public class DatabaseUserDetails implements org.springframework.security.core.userdetails.UserDetails {
    private UserDetails userDetails = null;

    public void setUser(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Optional.ofNullable(userDetails)
                .map(UserDetails::getRole)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .map(Collections::singleton)
                .orElseGet(Collections::emptySet);
    }

    @Override
    public String getPassword() {
        return Optional.ofNullable(userDetails)
                .map(UserDetails::getPassword)
                .orElse(null);
    }

    @Override
    public String getUsername() {
        return Optional.ofNullable(userDetails)
                .map(UserDetails::getUsername)
                .orElse(null);
    }

    public UserDetails getUserDetails() {
        return userDetails;
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
        return true;
    }
}

package com.xd.smartworksite.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final List<String> roles;
    private final List<String> permissions;
    private final Long defaultProjectId;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String username, List<String> roles,
                         List<String> permissions, Long defaultProjectId) {
        this.userId = userId;
        this.username = username;
        this.roles = roles == null ? List.of() : roles;
        this.permissions = permissions == null ? List.of() : permissions;
        this.defaultProjectId = defaultProjectId;
        this.authorities = Stream.concat(
                this.roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)),
                this.permissions.stream().map(SimpleGrantedAuthority::new)
        ).collect(Collectors.toList());
    }

    public Long getUserId() { return userId; }
    public List<String> getRoles() { return roles; }
    public List<String> getPermissions() { return permissions; }
    public Long getDefaultProjectId() { return defaultProjectId; }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}

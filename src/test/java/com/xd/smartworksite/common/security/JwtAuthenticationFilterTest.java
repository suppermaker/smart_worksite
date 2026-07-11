package com.xd.smartworksite.common.security;

import com.xd.smartworksite.auth.domain.UserAccount;
import com.xd.smartworksite.auth.mapper.UserAccountMapper;
import com.xd.smartworksite.common.redis.RedisCacheService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesOnlyWhenCurrentUserIsEnabled() throws ServletException, IOException {
        StaticJwtTokenService jwtTokenService = new StaticJwtTokenService();
        InMemoryUserAccountMapper userAccountMapper = new InMemoryUserAccountMapper();
        userAccountMapper.user = user(1L, "ENABLED");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService, noBlacklistRedis(), userAccountMapper);

        filter.doFilter(requestWithBearerToken(), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void disabledUserTokenDoesNotAuthenticate() throws ServletException, IOException {
        StaticJwtTokenService jwtTokenService = new StaticJwtTokenService();
        InMemoryUserAccountMapper userAccountMapper = new InMemoryUserAccountMapper();
        userAccountMapper.user = user(1L, "DISABLED");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService, noBlacklistRedis(), userAccountMapper);

        filter.doFilter(requestWithBearerToken(), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deletedUserTokenDoesNotAuthenticate() throws ServletException, IOException {
        StaticJwtTokenService jwtTokenService = new StaticJwtTokenService();
        InMemoryUserAccountMapper userAccountMapper = new InMemoryUserAccountMapper();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService, noBlacklistRedis(), userAccountMapper);

        filter.doFilter(requestWithBearerToken(), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWithBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private FilterChain noopChain() {
        return (request, response) -> {
        };
    }

    private RedisCacheService noBlacklistRedis() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        when(redisCacheService.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        return redisCacheService;
    }

    private UserAccount user(Long userId, String status) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername("user-" + userId);
        user.setStatus(status);
        return user;
    }

    private static class StaticJwtTokenService extends JwtTokenService {
        private final Claims claims = Jwts.claims()
                .subject("1")
                .add("username", "user-1")
                .add("roles", List.of("BUSINESS_USER"))
                .add("permissions", List.of("project:view"))
                .issuedAt(new Date())
                .build();

        StaticJwtTokenService() {
            super(jwtProperties());
        }

        @Override
        public Optional<Claims> parseToken(String token) {
            return Optional.of(claims);
        }
    }

    private static JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("12345678901234567890123456789012");
        properties.setExpiresInSeconds(3600);
        return properties;
    }

    private static class InMemoryUserAccountMapper implements UserAccountMapper {
        private UserAccount user;

        @Override public UserAccount selectByUsername(String username) { return null; }
        @Override public UserAccount selectById(Long userId) {
            return user != null && userId.equals(user.getId()) ? user : null;
        }
        @Override public List<UserAccount> selectPage(String keyword, String status) { return List.of(); }
        @Override public int insert(UserAccount user) { return 0; }
        @Override public int update(UserAccount user) { return 0; }
        @Override public int updatePassword(Long userId, String passwordHash) { return 0; }
        @Override public int updateStatus(Long userId, String status) { return 0; }
        @Override public int updateLastLoginAt(Long userId) { return 0; }
        @Override public List<String> selectRoleCodes(Long userId) { return List.of(); }
        @Override public List<String> selectPermissionCodes(Long userId) { return List.of(); }
        @Override public List<String> selectPermissionCodesByType(Long userId, String permissionType) { return List.of(); }
        @Override public Long selectDefaultProjectId(Long userId) { return null; }
    }
}

package com.xd.smartworksite.common.security;

import com.xd.smartworksite.common.redis.RedisCacheService;
import com.xd.smartworksite.common.redis.RedisKeys;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final RedisCacheService redisCacheService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, RedisCacheService redisCacheService) {
        this.jwtTokenService = jwtTokenService;
        this.redisCacheService = redisCacheService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            jwtTokenService.parseToken(token).ifPresent(claims -> {
                if (!isBlacklisted(claims)) {
                    UserPrincipal principal = jwtTokenService.toPrincipal(claims);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isBlacklisted(Claims claims) {
        String jti = claims.getId();
        if (jti == null) return false;
        return redisCacheService.get(RedisKeys.tokenBlacklist(jti)).isPresent();
    }
}

package com.xd.smartworksite.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_DEFAULT_PROJECT_ID = "defaultProjectId";

    private final SecretKey secretKey;
    private final long expiresInMillis;

    public JwtTokenService(JwtProperties props) {
        this.secretKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expiresInMillis = props.getExpiresInSeconds() * 1000L;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiresInMillis);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(principal.getUserId()))
                .claim(CLAIM_USERNAME, principal.getUsername())
                .claim(CLAIM_ROLES, principal.getRoles())
                .claim(CLAIM_DEFAULT_PROJECT_ID, principal.getDefaultProjectId())
                .claim(CLAIM_PERMISSIONS, principal.getPermissions())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public UserPrincipal toPrincipal(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        List<String> roles = getList(claims, CLAIM_ROLES);
        List<String> permissions = getList(claims, CLAIM_PERMISSIONS);
        Object dpId = claims.get(CLAIM_DEFAULT_PROJECT_ID);
        Long defaultProjectId = dpId == null ? null : Long.valueOf(dpId.toString());
        return new UserPrincipal(userId, username, roles, permissions, defaultProjectId);
    }

    public long getExpiresInSeconds() {
        return expiresInMillis / 1000L;
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Claims claims, String key) {
        Object value = claims.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}

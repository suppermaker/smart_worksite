package com.xd.smartworksite.common.security;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserFailsFastWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(SecurityUtils::getCurrentUser)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
                    assertThat(ex.getMessage()).isEqualTo("\u672a\u767b\u5f55");
                });
    }

    @Test
    void roleChecksUseAuthenticatedPrincipalAuthorities() {
        UserPrincipal principal = new UserPrincipal(7L, "admin", List.of("PLATFORM_ADMIN"), List.of("project:manage"), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(SecurityUtils.isPlatformAdmin()).isTrue();
        assertThat(SecurityUtils.hasRole("PLATFORM_ADMIN")).isTrue();
        assertThat(SecurityUtils.hasRole("PROJECT_USER")).isFalse();
    }
}

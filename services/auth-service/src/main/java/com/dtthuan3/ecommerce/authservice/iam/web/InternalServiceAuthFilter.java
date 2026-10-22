package com.dtthuan3.ecommerce.authservice.iam.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Service-to-service auth cho cac duong /internal/... (P2).
 *
 * <p>Gateway khong the mang JWT ADMIN cua user khi goi nguoc auth-service, nen cho phep
 * goi noi bo bang shared secret (header X-Internal-Token = gia tri iam.internal.token).
 * Public van bi chan: gateway denyAll moi path internal dang /api/{service}/internal/...
 * nen secret khong bao gio di qua public internet khi chay trong cluster; ngoai ra duong
 * internal van doi ROLE_ADMIN voi caller khong co secret.
 */
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String expectedToken;

    public InternalServiceAuthFilter(@Value("${iam.internal.token:}") String expectedToken) {
        this.expectedToken = expectedToken == null ? "" : expectedToken.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/") && !expectedToken.isEmpty()) {
            Authentication current = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = current != null
                    && current.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (!isAdmin && expectedToken.equals(request.getHeader(INTERNAL_TOKEN_HEADER))) {
                SecurityContextHolder.getContext()
                        .setAuthentication(new PreAuthenticatedAuthenticationToken(
                                "internal-service",
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
            }
        }
        chain.doFilter(request, response);
    }
}

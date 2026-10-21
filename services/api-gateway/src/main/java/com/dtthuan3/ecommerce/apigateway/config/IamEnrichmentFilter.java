package com.dtthuan3.ecommerce.apigateway.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * P2: enrich header IAM cho downstream (best-effort).
 *
 * <p>Lay sub + claims tu JWT da xac thuc, goi {@code GET /internal/auth/context/{sub}}
 * cua auth-service (kem X-Internal-Token, JIT provision neu sub la), roi gan
 * X-User-Id / X-Tenant-Id / X-Permissions. IAM cham (qua timeout), 404, hay loi bat ky:
 * chi log warn/debug, request van di tiep - khong bao gio chan nhu P0.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IamEnrichmentFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder iamWebClientBuilder;
    private final IamEnrichmentProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();
        if (path.contains("/internal/") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> enrich(exchange, chain, auth.getToken()))
                .switchIfEmpty(chain.filter(exchange))
                .onErrorResume(e -> chain.filter(exchange));
    }

    private Mono<Void> enrich(ServerWebExchange exchange, GatewayFilterChain chain, Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return chain.filter(exchange);
        }
        String uri = UriComponentsBuilder.fromPath("/internal/auth/context/{id}")
                .queryParamIfPresent("username", claim(jwt, "preferred_username", "username"))
                .queryParamIfPresent("email", claim(jwt, "email"))
                .queryParamIfPresent("fullName", claim(jwt, "name"))
                .buildAndExpand(subject)
                .toUriString();

        WebClient client = iamWebClientBuilder
                .baseUrl(properties.getAuthServiceUrl())
                .build();
        return client.get()
                .uri(uri)
                .header("X-Internal-Token", properties.getInternalToken())
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.empty())
                .bodyToMono(IamContextView.class)
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .flatMap(ctx -> chain.filter(mutate(exchange, ctx)))
                .switchIfEmpty(chain.filter(exchange))
                .onErrorResume(e -> {
                    log.warn("IAM enrich skipped path={} reason={}", exchange.getRequest().getPath(), e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private static java.util.Optional<String> claim(Jwt jwt, String... names) {
        for (String name : names) {
            try {
                String value = jwt.getClaimAsString(name);
                if (value != null && !value.isBlank()) {
                    return java.util.Optional.of(value);
                }
            } catch (Exception ignored) {
                // claim khong phai String - bo qua sang ten tiep theo
            }
        }
        return java.util.Optional.empty();
    }

    private static ServerWebExchange mutate(ServerWebExchange exchange, IamContextView ctx) {
        var builder = exchange.getRequest().mutate();
        if (ctx.getUserId() != null) {
            builder.header("X-User-Id", ctx.getUserId());
        }
        if (ctx.getTenantId() != null) {
            builder.header("X-Tenant-Id", String.valueOf(ctx.getTenantId()));
        } else if (ctx.getTenantCode() != null) {
            builder.header("X-Tenant-Id", ctx.getTenantCode());
        }
        if (ctx.getPermissions() != null && !ctx.getPermissions().isEmpty()) {
            builder.header("X-Permissions", String.join(",", ctx.getPermissions()));
        }
        return exchange.mutate().request(builder.build()).build();
    }

    @Override
    public int getOrder() {
        // Chay sau SecurityWebFilter (order -100) de chi enrich request da xac thuc.
        return -50;
    }
}

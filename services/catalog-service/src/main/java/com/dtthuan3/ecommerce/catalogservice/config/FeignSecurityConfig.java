package com.dtthuan3.ecommerce.catalogservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Configuration
public class FeignSecurityConfig {

    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor(OAuth2AuthorizedClientManager clientManager) {

        return requestTemplate -> {

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwt) {

                requestTemplate.header(
                        "Authorization",
                        "Bearer " + jwt.getToken().getTokenValue()
                );
            } else {
                var request = OAuth2AuthorizeRequest.withClientRegistrationId("catalog-search-reader")
                        .principal("catalog-search-projection")
                        .build();
                var client = clientManager.authorize(request);
                if (client == null) {
                    throw new IllegalStateException("Không lấy được access token cho Catalog search projection");
                }
                requestTemplate.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
            }
        };
    }

    @Bean
    public OAuth2AuthorizedClientManager catalogOAuth2AuthorizedClientManager(
            org.springframework.security.oauth2.client.registration.ClientRegistrationRepository registrations,
            org.springframework.security.oauth2.client.OAuth2AuthorizedClientService clients) {
        var provider = org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials().build();
        var manager = new org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations, clients);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }
}

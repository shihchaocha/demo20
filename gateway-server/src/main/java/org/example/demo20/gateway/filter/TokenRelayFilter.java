package org.example.demo20.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TokenRelayFilter implements GlobalFilter, Ordered {

    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    public TokenRelayFilter(ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .filter(p -> p instanceof OAuth2AuthenticationToken)
            .cast(OAuth2AuthenticationToken.class)
            .flatMap(auth -> authorizedClientService
                .<OAuth2AuthorizedClient>loadAuthorizedClient(
                    auth.getAuthorizedClientRegistrationId(), auth.getName()))
            .flatMap(client -> {
                String tokenValue = client.getAccessToken().getTokenValue();
                ServerHttpRequest decorated = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        super.getHeaders().forEach((name, values) -> {
                            if (!HttpHeaders.COOKIE.equalsIgnoreCase(name)) {
                                headers.put(name, values);
                            }
                        });
                        headers.setBearerAuth(tokenValue);
                        return headers;
                    }
                };
                return chain.filter(exchange.mutate().request(decorated).build());
            })
            .switchIfEmpty(chain.filter(exchange));
    }
}

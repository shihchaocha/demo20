package org.example.demo20.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
public class SecurityConfig {

    // Order 0: actuator endpoints are open (health checks, metrics scraping)
    @Bean
    @Order(0)
    public SecurityWebFilterChain managementSecurityFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"))
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll())
            .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

    // Default: all other routes require OAuth2 login
    @Bean
    public SecurityWebFilterChain gatewaySecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().authenticated())
            .oauth2Login(Customizer.withDefaults())
            .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }
}

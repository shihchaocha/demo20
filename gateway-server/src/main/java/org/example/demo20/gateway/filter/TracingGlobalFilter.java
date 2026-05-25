package org.example.demo20.gateway.filter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TracingGlobalFilter implements GlobalFilter, Ordered {

    private final OpenTelemetry openTelemetry;

    private static final TextMapGetter<ServerHttpRequest> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(ServerHttpRequest carrier) {
            return carrier.getHeaders().keySet();
        }
        @Override
        public String get(ServerHttpRequest carrier, String key) {
            return carrier.getHeaders().getFirst(key);
        }
    };

    private static final TextMapSetter<HttpHeaders> SETTER = HttpHeaders::set;

    public TracingGlobalFilter(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Context parentContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), exchange.getRequest(), GETTER);

        Span span = openTelemetry.getTracer("gateway-server")
                .spanBuilder(exchange.getRequest().getMethod().name() + " " + exchange.getRequest().getPath())
                .setParent(parentContext)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        Scope scope = span.makeCurrent();

        HttpHeaders tracingHeaders = new HttpHeaders();
        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), tracingHeaders, SETTER);

        HttpHeaders merged = new HttpHeaders();
        merged.addAll(exchange.getRequest().getHeaders());
        tracingHeaders.forEach((key, values) -> merged.put(key, values));

        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return merged;
            }
        };

        return chain.filter(exchange.mutate().request(decoratedRequest).build())
                .doFinally(signalType -> {
                    scope.close();
                    span.end();
                });
    }
}

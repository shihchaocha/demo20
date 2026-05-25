package org.example.demo20.frontend.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import org.springframework.stereotype.Component;

@Component
public class TracingFeignInterceptor implements RequestInterceptor {

    private final OpenTelemetry openTelemetry;

    public TracingFeignInterceptor(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void apply(RequestTemplate template) {
        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), template,
                        (carrier, key, value) -> carrier.header(key, value));
    }
}

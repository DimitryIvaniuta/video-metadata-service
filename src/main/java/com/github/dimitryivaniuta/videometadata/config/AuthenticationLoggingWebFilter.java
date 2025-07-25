package com.github.dimitryivaniuta.videometadata.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationLoggingWebFilter implements WebFilter {

    @NotNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(auth -> log.debug(
                        "Post‐auth for [{}]: principal={}, authorities={}",
                        path,
                        auth.getPrincipal(),
                        auth.getAuthorities()
                ))
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.debug("Post‐auth for [{}]: NO SecurityContext", path)))
                .then(chain.filter(exchange));
    }
}

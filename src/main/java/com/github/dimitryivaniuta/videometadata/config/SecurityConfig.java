package com.github.dimitryivaniuta.videometadata.config;

import com.github.dimitryivaniuta.videometadata.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central Spring Security configuration for the reactive stack.
 * <ul>
 *   <li>JWT resource-server with composite decoder (RS256 primary, HS256 legacy)</li>
 *   <li>JWT encoder backed by rotating RSA keys (JWK manager)</li>
 *   <li>Method security enabled (@PreAuthorize)</li>
 *   <li>CORS enabled (configure as needed for prod)</li>
 *   <li>Login endpoint open; everything under /api/** requires authentication</li>
 * </ul>
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(proxyTargetClass = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    private final JwkKeyManager jwkKeyManager;
    private final CompositeJwtDecoder compositeJwtDecoder;
    private final UserDetailsServiceImpl userDetailsService;
//    private final PasswordEncoder passwordEncoder;
//    private final SecurityJwtProperties jwtProperties;


/*    public SecurityConfig(SecurityJwtProperties jwtProperties,
                          ReactiveJwtDecoder jwtDecoder,
                          ReactiveJwtAuthenticationConverterAdapter jwtAuthConverter,
                          CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtProperties = jwtProperties;
        this.userDetailsService = userDetailsService;
        this.jwkKeyManager = jwkKeyManager;
        this.jwtDecoder = compositeJwtDecoder;
        this.jwtAuthConverter = jwtAuthConverter;
    }*/

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            ReactiveJwtAuthenticationConverterAdapter jwtAuthConverter,
//            CorsConfigurationSource corsConfigurationSource,
            AuthenticationLoggingWebFilter loggingFilter
    ) {
        return http
                // disable csrf for JWT-based APIs
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // route security
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.GET,  "/graphql/schema", "/api/graphql/schema").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/login", "/api/auth/login",
                                "/graphql", "/api/graphql").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/graphql/**", "/graphiql/**",
                                "/.well-known/jwks.json", "/api/.well-known/jwks.json").permitAll()
                        .anyExchange().authenticated()
//                        .pathMatchers("/api/**").authenticated()
//                        .anyExchange().denyAll()
                )
                // no formLogin or httpBasic
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // JWT validation for all others
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                )
                .addFilterAfter(loggingFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * CORS configuration. Adjust for production (restrict origins and headers).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // When allowCredentials=true, use allowedOriginPatterns with "*" if you need wildcard.
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
        // For production: replace patterns with concrete front-end origins.
    }

    /**
     * Decoder used by the resource server. We delegate to a composite that supports RS256 first and HS256 for legacy.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return compositeJwtDecoder;
    }

    /**
     * Encoder used to sign access tokens. Uses the rotating RSA key set managed by {@link JwkKeyManager}.
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        // NimbusJwtEncoder can take a JWKSource. JwkKeyManager exposes the rotating set.
        return new NimbusJwtEncoder(jwkKeyManager.getSigningJwkSourceDynamic());
    }

    /**
     * Convert JWT claims to GrantedAuthorities.
     * <p>
     * We merge:
     * <ul>
     *   <li>Custom {@code roles} claim (list or space-delimited) → {@code ROLE_*}</li>
     *   <li>Standard {@code scope}/{@code scp} claims → {@code SCOPE_*}</li>
     * </ul>
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        // Converter for scope/scp → SCOPE_*
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        // defaults: authoritiesClaimName = "scope" (space-delimited), fallbacks to "scp"
        // keep prefix "SCOPE_"
        // Converter for custom roles claim → ROLE_*
        var rolesConverter = (java.util.function.Function<Jwt, Collection<? extends GrantedAuthority>>) jwt -> {
            Object raw = jwt.getClaims().get("roles");
            if (raw == null) {
                return List.of();
            }
            List<String> roleNames;
            if (raw instanceof Collection<?> c) {
                roleNames = c.stream().map(Object::toString).toList();
            } else {
                roleNames = Arrays.stream(raw.toString().trim().split("\\s+"))
                        .filter(s -> !s.isBlank())
                        .toList();
            }
            return roleNames.stream()
                    .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
        };

        JwtAuthenticationConverter jwtAuth = new JwtAuthenticationConverter();
        jwtAuth.setJwtGrantedAuthoritiesConverter(jwt -> {
            // merge ROLE_* and SCOPE_* authorities
            Collection<GrantedAuthority> merged = new ArrayList<>();
            merged.addAll((Collection<? extends GrantedAuthority>) rolesConverter.apply(jwt));
            merged.addAll(scopes.convert(jwt));
            if (log.isDebugEnabled()) {
                log.debug("JWT authorities for sub='{}': {}", jwt.getSubject(), merged);
            }
            return merged;
        });

        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuth);
    }

    /** Auth manager backed by your UserDetailsServiceImpl + BCrypt */
    @Bean
    public ReactiveAuthenticationManager authenticationManager(PasswordEncoder enc) {
        UserDetailsRepositoryReactiveAuthenticationManager mgr =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        mgr.setPasswordEncoder(enc);
        return mgr;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LoggingPasswordEncoder();
    }

}

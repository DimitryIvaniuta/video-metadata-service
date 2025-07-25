package com.github.dimitryivaniuta.videometadata.config;

import com.github.dimitryivaniuta.videometadata.service.UserDetailsServiceImpl;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

import org.springframework.security.oauth2.jwt.*;

/**
 * Central Spring Security config:
 * - /auth/login open
 * - all other endpoints authenticated via JWT
 * - Reactive OAuth2 Resource Server (JWT)
 * - CORS, CSRF disabled
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(proxyTargetClass = true)
@Slf4j
public class SecurityConfig {

    private SecurityJwtProperties jwtProperties;

    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(SecurityJwtProperties jwtProperties,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtProperties = jwtProperties;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            ReactiveJwtAuthenticationConverterAdapter jwtAuthConverter,
            AuthenticationLoggingWebFilter loggingFilter
    ) {
        return http
                // disable csrf for JWT-based APIs
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // route security
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/graphql/**", "/graphiql/**").permitAll()
                        .anyExchange().authenticated()
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

    /** CORS config: allow all origins/methods/headers (tweak for prod as needed) */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("*"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /** Reactive JWT decoder using Nimbus + shared secret */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Build a NimbusJwtEncoder whose internal JWKSource contains exactly
     * one HS256 key with:
     *   • algorithm = HS256
     *   • use = signature
     *   • a key ID that we will reference in the JWS header
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        // Decode your Base64‑encoded 256‑bit secret
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        // Build the OctetSequence JWK
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey)
                .algorithm(JWSAlgorithm.HS256)      // must match header alg
                .keyUse(KeyUse.SIGNATURE)           // JWK use
                .keyID("videometadata-key")         // stable KID
                .build();

        // Wrap it in a one‐element JWKSet
        JWKSet jwkSet = new JWKSet(jwk);
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        // Finally create the encoder
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthConverter() {
        // 1) Create the authorities extractor
        JwtGrantedAuthoritiesConverter ga = new JwtGrantedAuthoritiesConverter();
        ga.setAuthorityPrefix("ROLE_");
        ga.setAuthoritiesClaimName("roles");

        // 2) Create the Authentication token converter and inject the above
        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(ga);

        // 3) Wrap in the reactive adapter
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthConverter);
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

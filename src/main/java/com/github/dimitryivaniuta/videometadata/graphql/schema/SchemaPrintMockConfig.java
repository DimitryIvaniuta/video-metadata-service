package com.github.dimitryivaniuta.videometadata.graphql.schema;

import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.type.filter.*;
import org.springframework.data.repository.Repository;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Configuration
@Profile("schema-print")                 // active only for ./gradlew printSchema
@Slf4j
public class SchemaPrintMockConfig {

    private static final String BASE_PKG =
            "com.github.dimitryivaniuta.videometadata";

    /** One post-processor that registers Mockito mocks for every missing bean. */
    @Bean
    public static BeanFactoryPostProcessor mockMissingBeans() {
        return bf -> {
            DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) bf;

            /* ── 1. Scan for @Service and Repository interfaces ────────────── */
//            var scanner = new ClassPathScanningCandidateComponentProvider(false);
//            scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
//            scanner.addIncludeFilter(new AssignableTypeFilter(Repository.class));
//
//            scanner.findCandidateComponents(BASE_PKG).forEach(def -> {
//                registerMockIfAbsent(dlbf, def.getBeanClassName());
//            });
            /* ── 1. Scan for @Service classes AND every Repository interface ───────── */
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false) {
                        @Override                              // <<< allow interfaces, too
                        protected boolean isCandidateComponent(
                                AnnotatedBeanDefinition bd) {
                            return true;                       // accept abstract + interfaces
                        }
                    };

            scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
            scanner.addIncludeFilter(new AssignableTypeFilter(
                    org.springframework.data.repository.Repository.class));

            scanner.findCandidateComponents(BASE_PKG)
                    .forEach(def -> registerMockIfAbsent(dlbf, def.getBeanClassName()));
            /* ── 2. Critical security mocks ───────────────────────────────── */
            ensureMock(dlbf, ReactiveAuthenticationManager.class,
                    "reactiveAuthManagerMock", invocation -> Mono.empty());

            ensureMock(dlbf, JwtEncoder.class,
                    "jwtEncoderMock", invocation -> { throw new UnsupportedOperationException(); });
        };
    }

    /** Dummy auth manager (never invoked while printing). */
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManagerMock() {
        return Mockito.mock(ReactiveAuthenticationManager.class,
                invocation -> Mono.empty());
    }

    /** Dummy JWT encoder so JwtTokenProvider can be constructed. */
    @Bean
    public JwtEncoder jwtEncoderMock() {
        return Mockito.mock(JwtEncoder.class, invocation -> {
            throw new UnsupportedOperationException("JWT encoding not used during schema printing");
        });
    }

    /** Satisfies UserCacheService */
    @Bean
    @SuppressWarnings("unchecked")
    public ReactiveRedisTemplate<String, Object> redisTemplateMock() {
        return Mockito.mock(ReactiveRedisTemplate.class);
    }

    /** Simple PasswordEncoder stub for UserServiceImpl. */
    @Bean
    public PasswordEncoder passwordEncoderStub() {
        // No-op encoder is enough; it won't be called during schema generation
        return NoOpPasswordEncoder.getInstance();
    }

    /** Precise generic signature so UserCacheService autowiring succeeds */
    @Bean
    @Primary                                  // make it the default candidate
    @SuppressWarnings("unchecked")
    public ReactiveRedisTemplate<String, CachedUser> cachedUserRedisTemplateMock() {
        log.debug("Schema-print: providing ReactiveRedisTemplate<CachedUser> mock");
        return Mockito.mock(ReactiveRedisTemplate.class);
    }

    /* …existing ReactiveAuthManager, JwtEncoder, PasswordEncoder mocks… */

    @Bean
    public R2dbcMappingContext r2dbcMappingContext() {
        return new R2dbcMappingContext();   // satisfies R2dbc repos
    }

    /* ---------- helpers ---------- */

    private static void registerMockIfAbsent(DefaultListableBeanFactory dlbf,
                                             String className) {

        try {
            Class<?> type = Class.forName(className);
            if (dlbf.getBeanNamesForType(type).length == 0) {
                dlbf.registerSingleton(className,
                        Mockito.mock(type, Mockito.RETURNS_DEFAULTS));
                log.debug("Schema-print: stubbed {}", className);
            }
        } catch (ClassNotFoundException ignored) { }
    }

    private static void ensureMock(DefaultListableBeanFactory dlbf,
                                   Class<?> type,
                                   String beanName,
                                   org.mockito.stubbing.Answer<?> answer) {

        if (dlbf.getBeanNamesForType(type).length == 0) {
            dlbf.registerSingleton(beanName, Mockito.mock(type, answer));
            log.debug("Schema-print: stubbed {}", type.getSimpleName());
        }
    }
}

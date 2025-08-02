package com.github.dimitryivaniuta.videometadata.graphql.schema;

import com.github.dimitryivaniuta.videometadata.config.GraphQLTypeMapper;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.*;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLArgument;
import com.github.dimitryivaniuta.videometadata.graphql.exceptions.GraphQlServiceException;
import com.github.dimitryivaniuta.videometadata.graphql.security.SecurityChecks;
import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.GraphQlSource;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.util.*;

@Configuration
@Slf4j
public class AnnotationSchemaFactory {

    private final ApplicationContext ctx;

    /**  Shared scalar registry – gives every scalar a single instance. */
    private static final Map<Class<?>, GraphQLScalarType> SCALARS = Map.of(
            Long.class,        ExtendedScalars.GraphQLLong,
            long.class,        ExtendedScalars.GraphQLLong,
            java.math.BigDecimal.class, ExtendedScalars.GraphQLBigDecimal,
            java.math.BigInteger.class, ExtendedScalars.GraphQLBigInteger,
            java.time.Instant.class,    ExtendedScalars.DateTime
    );

    private final GraphQLTypeMapper mapper = new GraphQLTypeMapper(SCALARS);

    public AnnotationSchemaFactory(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Bean
    public GraphQlSource graphQlSource() {

        GraphQLObjectType.Builder query    = GraphQLObjectType.newObject().name("Query");
        GraphQLObjectType.Builder mutation = GraphQLObjectType.newObject().name("Mutation");
        GraphQLCodeRegistry.Builder code   = GraphQLCodeRegistry.newCodeRegistry();

        // collect unique types we add
        Set<String> typeNames = new HashSet<>();

        scanBeanDefinitions(query, mutation, code, typeNames);
        ensureAtLeastOneQueryField(query, code);

        // add shared scalars once
        SCALARS.values().forEach(s -> addUnique(s, typeNames));

        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(query.build())
                .mutation(mutation.build())
                .codeRegistry(code.build())
//                .additionalTypes(new HashSet<>(SCALARS.values()))
                .build();

        return GraphQlSource.builder(schema).build();
    }

    /* ─ schema scanning ─ */

    private void scanBeanDefinitions(GraphQLObjectType.Builder query,
                                     GraphQLObjectType.Builder mutation,
                                     GraphQLCodeRegistry.Builder code,
                                     Set<String> typeNames) {

        for (String beanName : ctx.getBeanNamesForAnnotation(GraphQLApplication.class)) {

            Class<?> type = ctx.getType(beanName);
            if (type == null) continue;

            for (Method m : type.getMethods()) {

                GraphQLField fAnn = m.getAnnotation(GraphQLField.class);
                GraphQLMutation mutAnn = m.getAnnotation(GraphQLMutation.class);
                if (fAnn == null && mutAnn == null) continue;

                String field = !((fAnn != null ? fAnn.value() : mutAnn.value()).isBlank())
                        ? (fAnn != null ? fAnn.value() : mutAnn.value())
                        : m.getName();

                GraphQLOutputType out = mapper.toOutput(resolveReturn(m));
                addUnique(out, typeNames);

                List<graphql.schema.GraphQLArgument> args = buildArgs(m, typeNames);
                DataFetcher<?> fetcher = buildFetcher(beanName, m);

                GraphQLFieldDefinition def = GraphQLFieldDefinition.newFieldDefinition()
                        .name(field)
                        .type(out)
                        .arguments(args)
                        .build();

                if (fAnn != null) {
                    query.field(def);
                    code.dataFetcher(FieldCoordinates.coordinates("Query", field), fetcher);
                } else {
                    mutation.field(def);
                    code.dataFetcher(FieldCoordinates.coordinates("Mutation", field), fetcher);
                }
            }
        }
    }

    private void ensureAtLeastOneQueryField(GraphQLObjectType.Builder query,
                                            GraphQLCodeRegistry.Builder code) {

        if (!query.hasField("_status")) {
            GraphQLFieldDefinition status = GraphQLFieldDefinition.newFieldDefinition()
                    .name("_status")
                    .type(Scalars.GraphQLString)   // non-null? use GraphQLNonNull if needed
                    .build();

            query.field(status);

            code.dataFetcher(
                    FieldCoordinates.coordinates("Query", "_status"),
                    (DataFetcher<String>) env -> "OK"          // <-- explicit cast solves ambiguity
            );

            log.info("Inserted default Query._status field to satisfy GraphQL spec");
        }
    }

    /* ─ helpers: arguments & fetchers ─ */

    private List<graphql.schema.GraphQLArgument> buildArgs(Method m, Set<String> typeNames) {
        List<graphql.schema.GraphQLArgument> list = new ArrayList<>();
        for (Parameter p : m.getParameters()) {
            GraphQLArgument a = p.getAnnotation(GraphQLArgument.class);
            if (a == null) continue;
            GraphQLInputType in = mapper.toInput(p.getParameterizedType());
            addUnique(in, typeNames);
            list.add(graphql.schema.GraphQLArgument.newArgument()
                    .name(a.value())
                    .type(in)
                    .build());
        }
        return list;
    }

    private DataFetcher<?> buildFetcher(String beanName, Method m) {
        RequiresRole rr = m.getAnnotation(RequiresRole.class);
        return env -> {
            Mono<Void> guard = rr == null
                    ? Mono.empty()
                    : SecurityChecks.requireAnyRole(rr.value());

            Mono<Object> exec = Mono.defer(() -> {
                Object bean = ctx.getBean(beanName);   // instantiate lazily (no cycle)
                return invoke(bean, m, env);
            });
            return guard.then(exec);
        };
    }

    /* ─ invoke bean method reflectively  */

    @SuppressWarnings("unchecked")
    private static Mono<Object> invoke(Object bean, Method m, DataFetchingEnvironment env) {
        try {
            Object[] args = resolveInvokeArgs(m, env);
            Object    res = m.invoke(bean, args);

            if (res instanceof Mono<?>      mono) return (Mono<Object>) mono;
            if (res instanceof Publisher<?> pub)  return Mono.from((Publisher<?>) pub);
            return Mono.justOrEmpty(res);

        } catch (InvocationTargetException ite) {
//            Throwable c = ite.getTargetException();
//            return Mono.error(c instanceof RuntimeException re ? re
//                    : new GraphQlServiceException("Invocation error", c));
            Throwable real = ite.getTargetException();
            // log it so you see the stack trace
            log.error("Error invoking GraphQL method {}", m.getName(), real);
            throw new GraphQlServiceException("Invocation error", ite.getTargetException());
        } catch (Exception ex) {
            return Mono.error(new GraphQlServiceException("Invocation error", ex));
        }
    }

    private static Object[] resolveInvokeArgs(Method m, DataFetchingEnvironment env) {
        Object[] args = new Object[m.getParameterCount()];
        Parameter[] ps = m.getParameters();
        for (int i=0;i<ps.length;i++) {
            Parameter p = ps[i];
            GraphQLArgument a = p.getAnnotation(GraphQLArgument.class);
            if (a != null) {
                Object v = env.getArgument(a.value());
                if (p.isAnnotationPresent(NotBlank.class)
                        && (!(v instanceof String s) || s.isBlank())) {
                    throw new GraphQlServiceException("Argument '"+a.value()+"' must not be blank");
                }
                args[i] = v;
            } else if (p.getType().isAssignableFrom(DataFetchingEnvironment.class)) {
                args[i] = env;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    /*  util: type deduplication ─ */
/*

    private static void addUnique(GraphQLType t, Set<String> names) {
        if (t instanceof GraphQLNamedType n) {
            if (!names.add(n.getName())) {
                log.error("Duplicate GraphQL type name detected: {}", n.getName());
                throw new IllegalStateException("Duplicate type: " + n.getName());
            } else {
                log.debug("Registered GraphQL type: {}", n.getName());
            }
        }
    }
*/
    private static void addUnique(GraphQLType t, Set<String> names) {
        if (t instanceof GraphQLNamedType n) {
            if (!names.add(n.getName())) {
                // already registered; but if it's the SAME instance, that's fine
                return;
            }
            log.debug("Registered GraphQL type {}", n.getName());
        }
    }
    /* return-type resolver */
    private static Type resolveReturn(Method m) {
        Type t = m.getGenericReturnType();
        if (t instanceof ParameterizedType pt) {
            Class<?> raw = (Class<?>) pt.getRawType();
            if ((raw == Mono.class || raw == Publisher.class) && pt.getActualTypeArguments().length == 1) {
                return pt.getActualTypeArguments()[0];
            }
        }
        return t;
    }
}

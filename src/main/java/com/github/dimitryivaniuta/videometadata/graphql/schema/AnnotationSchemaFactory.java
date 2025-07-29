package com.github.dimitryivaniuta.videometadata.graphql.schema;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.*;
import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLArgument;
import com.github.dimitryivaniuta.videometadata.graphql.exceptions.GraphQlServiceException;
import com.github.dimitryivaniuta.videometadata.graphql.security.SecurityChecks;
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

/**
 * Builds a code‑first GraphQL schema from beans annotated with {@link GraphQLApplication}.
 * Uses {@link GraphQlSource.SchemaResourceBuilder} to register scalars and supply the schema.
 */
@Configuration
@Slf4j
public class AnnotationSchemaFactory {

    private final ApplicationContext ctx;
    private final GraphQLTypeMapper  mapper = new GraphQLTypeMapper();

    public AnnotationSchemaFactory(ApplicationContext ctx) { this.ctx = ctx; }

    @Bean
    public GraphQlSource graphQlSource() {

        GraphQLObjectType.Builder query    = GraphQLObjectType.newObject().name("Query");
        GraphQLObjectType.Builder mutation = GraphQLObjectType.newObject().name("Mutation");
        GraphQLCodeRegistry.Builder code   = GraphQLCodeRegistry.newCodeRegistry();

        scanBeans(query, mutation, code);

        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(query.build())
                .mutation(mutation.build())
                .codeRegistry(code.build())
                .additionalType(ExtendedScalars.DateTime)
                .additionalType(ExtendedScalars.GraphQLLong)
                .additionalType(ExtendedScalars.GraphQLBigDecimal)
                .additionalType(ExtendedScalars.GraphQLBigInteger)
                .build();

        // Use SchemaResourceBuilder so we get configureRuntimeWiring(...)
        return GraphQlSource.schemaResourceBuilder()
                .configureRuntimeWiring(wiring -> wiring
                        .scalar(ExtendedScalars.DateTime)
                        .scalar(ExtendedScalars.GraphQLLong)
                        .scalar(ExtendedScalars.GraphQLBigDecimal)
                        .scalar(ExtendedScalars.GraphQLBigInteger))
                .schemaFactory((__, ___) -> schema)
                .build();
    }

    /* --------------------------------------------------------------------- */
    /*  Reflection scanning and DataFetcher wiring                           */
    /* --------------------------------------------------------------------- */
    private void scanBeans(GraphQLObjectType.Builder query,
                           GraphQLObjectType.Builder mutation,
                           GraphQLCodeRegistry.Builder code) {

        for (String beanName : ctx.getBeanNamesForAnnotation(GraphQLApplication.class)) {
            Object bean = ctx.getBean(beanName);

            for (Method m : bean.getClass().getMethods()) {
                GraphQLField fieldAnn   = m.getAnnotation(GraphQLField.class);
                GraphQLMutation mutAnn  = m.getAnnotation(GraphQLMutation.class);
                if (fieldAnn == null && mutAnn == null) continue;

                String fname = (fieldAnn != null ? fieldAnn.value() : mutAnn.value());
                if (fname.isBlank()) fname = m.getName();

                GraphQLOutputType outType = mapper.toOutput(resolveReturn(m));

                List<graphql.schema.GraphQLArgument> gqlArgs = new ArrayList<>();
                for (Parameter p : m.getParameters()) {
                    GraphQLArgument a = p.getAnnotation(GraphQLArgument.class);
                    if (a == null) continue;
                    gqlArgs.add(graphql.schema.GraphQLArgument.newArgument()
                            .name(a.value())
                            .type(mapper.toInput(p.getParameterizedType()))
                            .build());
                }

                DataFetcher<?> df = buildFetcher(bean, m);

                GraphQLFieldDefinition def = GraphQLFieldDefinition.newFieldDefinition()
                        .name(fname)
                        .type(outType)
                        .arguments(gqlArgs)
                        .build();

                if (fieldAnn != null) {
                    query.field(def);
                    code.dataFetcher(FieldCoordinates.coordinates("Query", fname), df);
                } else {
                    mutation.field(def);
                    code.dataFetcher(FieldCoordinates.coordinates("Mutation", fname), df);
                }
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Fetcher with role guard & validation                                 */
    /* --------------------------------------------------------------------- */
    private DataFetcher<?> buildFetcher(Object bean, Method m) {
        RequiresRole roleAnn = m.getAnnotation(RequiresRole.class);

        return env -> {
            Mono<Void> guard = roleAnn == null
                    ? Mono.empty()
                    : SecurityChecks.requireAnyRole(roleAnn.value());

            Mono<Object> inv = Mono.defer(() -> invoke(bean, m, env));
            return guard.then(inv);
        };
    }

    private static Mono<Object> invoke(Object bean, Method m, DataFetchingEnvironment env) {
        try {
            Object[] args = new Object[m.getParameterCount()];
            Parameter[] ps = m.getParameters();

            for (int i = 0; i < ps.length; i++) {
                Parameter p = ps[i];
                GraphQLArgument ga = p.getAnnotation(GraphQLArgument.class);

                if (ga != null) {
                    Object v = env.getArgument(ga.value());

                    if (p.isAnnotationPresent(NotBlank.class) &&
                            (!(v instanceof String s) || s.isBlank())) {
                        return Mono.error(new GraphQlServiceException(
                                "Argument '" + ga.value() + "' must not be blank"));
                    }
                    args[i] = v;
                } else if (p.getType().isAssignableFrom(DataFetchingEnvironment.class)) {
                    args[i] = env;
                } else {
                    args[i] = null;
                }
            }

            Object res = m.invoke(bean, args);
            if (res instanceof Mono<?> mono)           return (Mono<Object>) mono;
            if (res instanceof Publisher<?> pub)       return Mono.from((Publisher<?>) pub);
            return Mono.justOrEmpty(res);

        } catch (InvocationTargetException ie) {
            Throwable c = ie.getTargetException();
            return Mono.error(c instanceof RuntimeException re ? re
                    : new GraphQlServiceException("Invocation error", c));
        } catch (Exception ex) {
            return Mono.error(new GraphQlServiceException("Invocation error", ex));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Helpers                                                              */
    /* --------------------------------------------------------------------- */
    private static Type resolveReturn(Method m) {
        Type t = m.getGenericReturnType();
        Class<?> raw = raw(t);
        if ((raw == Mono.class || raw == Publisher.class) && t instanceof ParameterizedType pt) {
            return pt.getActualTypeArguments()[0];
        }
        return t;
    }
    private static Class<?> raw(Type t) {
        if (t instanceof Class<?> c) return c;
        if (t instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        if (t instanceof GenericArrayType ga)
            return java.lang.reflect.Array.newInstance(raw(ga.getGenericComponentType()), 0).getClass();
        return Object.class;
    }
}

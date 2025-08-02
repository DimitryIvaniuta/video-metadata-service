package com.github.dimitryivaniuta.videometadata.config;

import com.github.dimitryivaniuta.videometadata.graphql.annotations.GraphQLIgnore;
import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps Java types -> GraphQL input/output types.
 * <p>
 * You can pass in a shared scalar registry so every scalar instance
 * is reused exactly once across the whole schema (prevents
 * “duplicate type name” cycles).
 */
@Slf4j
public final class GraphQLTypeMapper {

    /* ───────────────────── default scalars ────────────────────── */

    private static Map<Class<?>, GraphQLScalarType> defaultScalars() {
        Map<Class<?>, GraphQLScalarType> m = new HashMap<>();
        m.put(String.class, Scalars.GraphQLString);

        m.put(Integer.class, Scalars.GraphQLInt);
        m.put(int.class, Scalars.GraphQLInt);
        m.put(Boolean.class, Scalars.GraphQLBoolean);
        m.put(boolean.class, Scalars.GraphQLBoolean);
        m.put(Double.class, Scalars.GraphQLFloat);
        m.put(double.class, Scalars.GraphQLFloat);
        m.put(Float.class, Scalars.GraphQLFloat);
        m.put(float.class, Scalars.GraphQLFloat);

        m.put(Long.class, ExtendedScalars.GraphQLLong);
        m.put(long.class, ExtendedScalars.GraphQLLong);
        m.put(BigDecimal.class, ExtendedScalars.GraphQLBigDecimal);
        m.put(BigInteger.class, ExtendedScalars.GraphQLBigInteger);

        m.put(Instant.class, ExtendedScalars.DateTime);
        m.put(LocalDateTime.class, ExtendedScalars.DateTime);
        m.put(OffsetDateTime.class, ExtendedScalars.DateTime);
        m.put(LocalDate.class, ExtendedScalars.DateTime);

        m.put(UUID.class, Scalars.GraphQLID);
        return m;
    }

    /* ───────────────────── instance members ───────────────────── */

    private final Map<Class<?>, GraphQLScalarType> scalars;
    private final Map<Class<?>, GraphQLEnumType> enumCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, GraphQLOutputType> outCache = new ConcurrentHashMap<>();
//    private final Map<Class<?>, GraphQLInputType>  inCache    = new ConcurrentHashMap<>();

    /* ───────────────────── constructors ───────────────────────── */

    /**
     * Uses built-in scalars only.
     */
    public GraphQLTypeMapper() {
        this(Collections.emptyMap());
    }

    /**
     * @param sharedScalars external registry (e.g. from AnnotationSchemaFactory)
     *                      The mapper reuses these instances to avoid duplicates.
     */
    public GraphQLTypeMapper(Map<Class<?>, GraphQLScalarType> sharedScalars) {
        Map<Class<?>, GraphQLScalarType> merged = new HashMap<>(defaultScalars());
        if (sharedScalars != null) {
            sharedScalars.forEach(merged::put);
        }
        this.scalars = Collections.unmodifiableMap(merged);
    }

    /* ───────────────────── public API ─────────────────────────── */

    public GraphQLOutputType toOutput(Type generic) {
        return mapOutput(generic, new HashSet<>());
    }

    public GraphQLInputType toInput(Type generic) {
        return mapInput(generic, new HashSet<>());
    }

    /* ───────────────────── output mapping ─────────────────────── */
    private GraphQLOutputType mapOutput(Type src, Set<Class<?>> guard) {
        Class<?> raw = raw(src);

        // 1) Arrays or concrete Java collections → GraphQLList
        if (raw.isArray() || Collection.class.isAssignableFrom(raw)) {
            return GraphQLList.list(mapOutput(innerType(src, 0, raw), guard));
        }

        // 2) Reactive wrappers
        //    • Flux / Publisher  → LIST of T
        //    • Mono / Optional   → just T
        if (Flux.class.isAssignableFrom(raw)
                || (Publisher.class.isAssignableFrom(raw)
                && !Mono.class.isAssignableFrom(raw))) {
            return GraphQLList.list(mapOutput(innerType(src, 0, raw), guard));
        }
        if (Mono.class.isAssignableFrom(raw) || Optional.class.isAssignableFrom(raw)) {
            return mapOutput(innerType(src, 0, raw), guard);
        }

        // 3) Scalars
        GraphQLScalarType scalar = scalars.get(raw);
        if (scalar != null) return scalar;

        // 4) Enums
        if (raw.isEnum()) {
            return enumCache.computeIfAbsent(raw, this::buildEnum);
        }

        // 5) Record / POJO objects
        return outCache.computeIfAbsent(raw, c -> buildObject(c, guard));
    }

    /* ───────────────────── input mapping ──────────────────────── */

    private GraphQLInputType mapInput(Type src, Set<Class<?>> guard) {
        Class<?> raw = raw(src);

        if (raw.isArray() || Collection.class.isAssignableFrom(raw)) {
            return GraphQLList.list(mapInput(innerType(src, 0, raw), guard));
        }
        if (Optional.class.isAssignableFrom(raw)) {
            return mapInput(innerType(src, 0, raw), guard);
        }

        GraphQLScalarType scalar = scalars.get(raw);
        if (scalar != null) return scalar;

        if (raw.isEnum()) {
            // Reuse the SAME enum instance for input & output
            return enumCache.computeIfAbsent(raw, this::buildEnum);
        }

        // complex input: fall back to String
        return Scalars.GraphQLString;
    }

    /* ───────────────────── enum builder ───────────────────────── */

/*    private GraphQLEnumType buildEnum(Class<?> e) {
        GraphQLEnumType.Builder b = GraphQLEnumType.newEnum().name(e.getSimpleName());
        for (Object c : e.getEnumConstants()) {
            b.value(((Enum<?>) c).name());
        }
        return b.build();
    }*/
private GraphQLEnumType buildEnum(Class<?> e) {
    GraphQLEnumType.Builder b = GraphQLEnumType.newEnum()
            .name(e.getSimpleName());
    for (Object c : e.getEnumConstants()) {
        String literal = ((Enum<?>) c).name();
        b.value(
                GraphQLEnumValueDefinition.newEnumValueDefinition()
                        .name(literal)
                        .value(c)               // map back to enum constant
                        .build()
        );
    }
    return b.build();
}

    /* ───────────────────── object builder ─────────────────────── */

    private GraphQLOutputType buildObject(Class<?> clz, Set<Class<?>> guard) {
        if (!guard.add(clz)) { // recursion -> reference
            return GraphQLTypeReference.typeRef(clz.getSimpleName());
        }

        GraphQLObjectType.Builder ob = GraphQLObjectType.newObject().name(clz.getSimpleName());

        Map<String, Type> props = collectProperties(clz);

        props.forEach((name, type) -> {
            GraphQLOutputType out = mapOutput(type, guard);
            if ("id".equals(name)) {
                out = Scalars.GraphQLID;
            }
            ob.field(GraphQLFieldDefinition.newFieldDefinition()
                    .name(name)
                    .type(out)
                    .build());
        });

        guard.remove(clz);
        return ob.build();
    }

    private Map<String, Type> collectProperties(Class<?> clz) {
        Map<String, Type> props = new LinkedHashMap<>();

        // record components
        if (clz.isRecord()) {
            for (RecordComponent rc : clz.getRecordComponents()) {
                props.put(rc.getName(), rc.getGenericType());
            }
        }
        // getters
        for (Method m : clz.getMethods()) {

            if (m.isAnnotationPresent(GraphQLIgnore.class)) continue;

            if (isGetter(m)) {
                props.putIfAbsent(toFieldName(m.getName()), m.getGenericReturnType());
            }
        }
        // public fields
        for (Field f : clz.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                props.putIfAbsent(f.getName(), f.getGenericType());
            }
        }
        return props;
    }

    /* ───────────────────── helper utils ───────────────────────── */

    private static Class<?> raw(Type t) {
        if (t instanceof Class<?> c) return c;
        if (t instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        if (t instanceof GenericArrayType ga) {
            Class<?> comp = raw(ga.getGenericComponentType());
            return Array.newInstance(comp, 0).getClass();
        }
        return Object.class;
    }

    private static Type innerType(Type src, int index, Class<?> raw) {
        if (raw.isArray()) return raw.getComponentType();
        if (src instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (index < args.length) return args[index];
        }
        return Object.class;
    }

    private static boolean isGetter(Method m) {
        // skip Object.class methods
        if (m.getDeclaringClass() == Object.class) return false;

        String name = m.getName();
        if ("getClass".equals(name)) return false;          // stops Module recursion

        return Modifier.isPublic(m.getModifiers())
                && m.getParameterCount() == 0
                && !m.getReturnType().equals(Void.TYPE)
                && (name.startsWith("get") || name.startsWith("is"));
    }

    private static String toFieldName(String method) {
        if (method.startsWith("get") && method.length() > 3) {
            return decap(method.substring(3));
        }
        if (method.startsWith("is") && method.length() > 2) {
            return decap(method.substring(2));
        }
        return method;
    }

    private static String decap(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}

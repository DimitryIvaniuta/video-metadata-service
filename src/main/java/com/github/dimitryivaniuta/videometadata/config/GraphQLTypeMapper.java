package com.github.dimitryivaniuta.videometadata.config;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;

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
 * Reflection‑based Java -> GraphQL type mapper.
 * Thread‑safe and recursion‑safe. Re‑uses graphql‑java ExtendedScalars.
 */
public final class GraphQLTypeMapper {

    // scalar lookup
    private static final Map<Class<?>, GraphQLScalarType> SCALARS;
    static {
        Map<Class<?>, GraphQLScalarType> m = new HashMap<>();
        m.put(String.class, Scalars.GraphQLString);

        m.put(Integer.class, Scalars.GraphQLInt);  m.put(int.class, Scalars.GraphQLInt);
        m.put(Boolean.class, Scalars.GraphQLBoolean);  m.put(boolean.class, Scalars.GraphQLBoolean);
        m.put(Double.class, Scalars.GraphQLFloat);  m.put(double.class, Scalars.GraphQLFloat);
        m.put(Float.class, Scalars.GraphQLFloat);   m.put(float.class, Scalars.GraphQLFloat);

        m.put(Long.class, ExtendedScalars.GraphQLLong);  m.put(long.class, ExtendedScalars.GraphQLLong);
        m.put(BigDecimal.class, ExtendedScalars.GraphQLBigDecimal);
        m.put(BigInteger.class, ExtendedScalars.GraphQLBigInteger);

        // java‑time scalars -> use DateTime from extended‑scalars
        m.put(Instant.class,          ExtendedScalars.DateTime);
        m.put(LocalDateTime.class,    ExtendedScalars.DateTime);
        m.put(OffsetDateTime.class,   ExtendedScalars.DateTime);
        m.put(LocalDate.class,        ExtendedScalars.DateTime);

        m.put(UUID.class, Scalars.GraphQLID);

        SCALARS = Map.copyOf(m);
    }

    // caches
    private final Map<Class<?>, GraphQLOutputType> outputCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, GraphQLInputType>  inputCache  = new ConcurrentHashMap<>();

    // public API
    public GraphQLOutputType toOutput(Type generic) {
        return mapOutput(generic, new HashSet<>());
    }

    public GraphQLInputType toInput(Type generic) {
        return mapInput(generic, new HashSet<>());
    }

    // Mapping logic (output)
    private GraphQLOutputType mapOutput(Type src, Set<Class<?>> guard) {
        Class<?> raw = raw(src);

        // unwrap reactive / optional / array / collection
        if (unwrapTypes(raw)) {
            Type inner = innerType(src, 0, raw);
            return GraphQLList.list(mapOutput(inner, guard));
        }
        if (Optional.class.isAssignableFrom(raw)
                || Mono.class.isAssignableFrom(raw)
                || Flux.class.isAssignableFrom(raw)
                || Publisher.class.isAssignableFrom(raw)) {
            Type inner = innerType(src, 0, raw);
            return mapOutput(inner, guard);
        }

        // scalar?
        GraphQLScalarType scalar = SCALARS.get(raw);
        if (scalar != null) return scalar;

        // enum?
        if (raw.isEnum()) {
            return outputCache.computeIfAbsent(raw, this::buildEnum);
        }

        // POJO / record
        return outputCache.computeIfAbsent(raw, c -> buildObject(c, guard));
    }

    private boolean unwrapTypes(Class<?> raw) {
        return raw.isArray() || Collection.class.isAssignableFrom(raw);
    }

    // Mapping logic (input)
    private GraphQLInputType mapInput(Type src, Set<Class<?>> guard) {
        Class<?> raw = raw(src);

        if (unwrapTypes(raw)) {
            Type inner = innerType(src, 0, raw);
            return GraphQLList.list(mapInput(inner, guard));
        }
        if (Optional.class.isAssignableFrom(raw)) {
            return mapInput(innerType(src, 0, raw), guard);
        }

        GraphQLScalarType scalar = SCALARS.get(raw);
        if (scalar != null) return scalar;

        if (raw.isEnum()) {
            return inputCache.computeIfAbsent(raw, c -> (GraphQLInputType) buildEnum(c));
        }

        // Complex input – fallback to String (or build input object the same way if needed)
        return Scalars.GraphQLString;
    }

    // Builders
    private GraphQLEnumType buildEnum(Class<?> e) {
        GraphQLEnumType.Builder b = GraphQLEnumType.newEnum().name(e.getSimpleName());
        for (Object c : e.getEnumConstants()) {
            b.value(((Enum<?>) c).name());
        }
        return b.build();
    }

    private GraphQLOutputType buildObject(Class<?> clz, Set<Class<?>> guard) {
        if (!guard.add(clz)) { // recursion -> return type reference
            return GraphQLTypeReference.typeRef(clz.getSimpleName());
        }

        GraphQLObjectType.Builder ob = GraphQLObjectType.newObject().name(clz.getSimpleName());

        // gather property readers
        Map<String, Type> props = new LinkedHashMap<>();

        // record components
        if (clz.isRecord()) {
            for (RecordComponent rc : clz.getRecordComponents()) {
                props.put(rc.getName(), rc.getGenericType());
            }
        }

        // public getters
        for (Method m : clz.getMethods()) {
            if (isGetter(m)) {
                props.putIfAbsent(toFieldName(m.getName()), m.getGenericReturnType());
            }
        }

        // public fields
        for (Field f : clz.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) continue;
            props.putIfAbsent(f.getName(), f.getGenericType());
        }

        // build fields
        for (Map.Entry<String, Type> p : props.entrySet()) {
            String fname = p.getKey();
            Type   ftype = p.getValue();

            GraphQLOutputType out = mapOutput(ftype, guard);
            if (fname.equals("id")) {
                out = Scalars.GraphQLID;
            }

            ob.field(GraphQLFieldDefinition.newFieldDefinition()
                    .name(fname)
                    .type(out)
                    .build());
        }

        guard.remove(clz);
        return ob.build();
    }

    // Helpers
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
        return Modifier.isPublic(m.getModifiers())
                && m.getParameterCount() == 0
                && !m.getReturnType().equals(Void.TYPE)
                && (m.getName().startsWith("get") || m.getName().startsWith("is"));
    }

    /**
     * Turns JavaBean getter names into field names: getName -> name, isActive -> active.
     */
    private static String toFieldName(String method) {
        if (method.startsWith("get") && method.length() > 3) {
            String n = method.substring(3);
            return decap(n);
        }
        if (method.startsWith("is") && method.length() > 2) {
            String n = method.substring(2);
            return decap(n);
        }
        return method;
    }
    private static String decap(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}

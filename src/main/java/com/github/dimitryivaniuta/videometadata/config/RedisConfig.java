package com.github.dimitryivaniuta.videometadata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.dimitryivaniuta.videometadata.web.dto.CachedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Profile("!schema-print")
@Configuration
public class RedisConfig {

    /**
     * Dedicated ObjectMapper for Redis payloads.
     * Registers JavaTimeModule and writes ISO-8601 strings for Instants.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Reactive Redis template for caching {@link CachedUser}.
     * Uses String keys and a typed JSON serializer for values.
     * No deprecated APIs are used.
     */
    @Bean
    public ReactiveRedisTemplate<String, CachedUser> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper redisObjectMapper) {

        // Key serializer
        RedisSerializer<String> keySerializer = new StringRedisSerializer();

        // Typed value serializer (JSON via Jackson) with JavaTimeModule support
        RedisSerializer<CachedUser> valueSerializer = new CachedUserRedisSerializer(redisObjectMapper);

        // Build serialization context
        RedisSerializationContext<String, CachedUser> context =
                RedisSerializationContext.<String, CachedUser>newSerializationContext(keySerializer)
                        .value(valueSerializer)
                        .hashValue(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    /**
     * Typed RedisSerializer for CachedUser using Jackson.
     * Avoids deprecated setObjectMapper and preserves proper Java time handling.
     */
    static final class CachedUserRedisSerializer implements RedisSerializer<CachedUser> {

        private final ObjectMapper mapper;

        CachedUserRedisSerializer(ObjectMapper base) {
            // copy to avoid mutating the global mapper
            this.mapper = base.copy()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        @Override
        public byte[] serialize(CachedUser value) throws SerializationException {
            if (value == null) {
                return new byte[0];
            }
            try {
                return mapper.writeValueAsBytes(value);
            } catch (Exception e) {
                throw new SerializationException("Failed to serialize CachedUser", e);
            }
        }

        @Override
        public CachedUser deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            try {
                return mapper.readValue(bytes, CachedUser.class);
            } catch (Exception e) {
                throw new SerializationException("Failed to deserialize CachedUser", e);
            }
        }
    }
}

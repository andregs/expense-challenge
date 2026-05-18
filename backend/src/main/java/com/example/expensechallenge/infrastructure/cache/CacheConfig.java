package com.example.expensechallenge.infrastructure.cache;

import com.example.expensechallenge.service.FxRate;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Enables Spring's caching abstraction and tunes the default
 * {@link RedisCacheConfiguration} for FX-rate entries.
 *
 * <p>The provided configuration is picked up by Spring Boot's
 * auto-configured {@code RedisCacheManager} and applied to every cache
 * unless a per-cache override is registered. Values are serialised as
 * JSON via the injected {@link JsonMapper} (Jackson 3) — the same
 * mapper that wires the strict deserialisation rules in
 * {@code JacksonConfig} — so cached payloads are human-readable in
 * {@code redis-cli} and survive class-internal refactors as long as
 * field names hold. Today the only cache is {@code fxRates}, holding
 * {@link FxRate} records; adding additional caches with different value
 * types will require widening the deserialiser or registering per-cache
 * configurations.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(JsonMapper jsonMapper, FxCacheConfig props) {
        RedisSerializer<Object> jsonSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(Object value) {
                return value == null ? new byte[0] : jsonMapper.writeValueAsBytes(value);
            }

            @Override
            public Object deserialize(byte[] bytes) {
                return (bytes == null || bytes.length == 0)
                    ? null
                    : jsonMapper.readValue(bytes, FxRate.class);
            }
        };

        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(props.ttl())
            .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer));
    }
}

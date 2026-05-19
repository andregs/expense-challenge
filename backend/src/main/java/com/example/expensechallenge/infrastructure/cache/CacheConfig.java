package com.example.expensechallenge.infrastructure.cache;

import com.example.expensechallenge.service.FxRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
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
 * <p>A custom {@link CacheErrorHandler} replaces Spring's default
 * {@link SimpleCacheErrorHandler} (which rethrows cache exceptions).
 * Cache errors are logged as warnings and silently ignored, so a Redis
 * outage degrades gracefully: FX lookups hit the Treasury API on every
 * request instead of failing outright. No stale data is served — the
 * degradation is purely a performance hit, not a correctness issue.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig implements org.springframework.cache.annotation.CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

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

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis GET failed [cache={} key={}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed [cache={} key={}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis EVICT failed [cache={} key={}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis CLEAR failed [cache={}]: {}", cache.getName(), e.getMessage());
            }
        };
    }
}

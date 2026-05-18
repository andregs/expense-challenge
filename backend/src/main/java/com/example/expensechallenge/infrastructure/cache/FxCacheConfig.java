package com.example.expensechallenge.infrastructure.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fx-cache")
public record FxCacheConfig(Duration ttl) {}

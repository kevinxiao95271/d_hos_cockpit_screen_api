package com.kxhospital.cockpit.ds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class DsCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        try {
            factory.getConnection().close();
            log.info(">>> Redis 连接成功，启用 Redis 缓存");
        } catch (Exception e) {
            log.warn(">>> Redis 不可用 ({}), 降级为本地内存缓存", e.getMessage());
            return new ConcurrentMapCacheManager(
                "dsRegions", "dsTasks", "dsStatTrend", "dsStatSummary",
                "dsProvince", "dsCity", "dsOrgs"
            );
        }

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("dsRegions",     base.entryTtl(Duration.ofHours(2)));
        configs.put("dsTasks",       base.entryTtl(Duration.ofMinutes(5)));
        configs.put("dsStatTrend",   base.entryTtl(Duration.ofMinutes(3)));
        configs.put("dsStatSummary", base.entryTtl(Duration.ofMinutes(3)));
        configs.put("dsProvince",    base.entryTtl(Duration.ofMinutes(3)));
        configs.put("dsCity",        base.entryTtl(Duration.ofMinutes(3)));
        configs.put("dsOrgs",        base.entryTtl(Duration.ofMinutes(3)));

        return RedisCacheManager.builder(factory)
                .withInitialCacheConfigurations(configs)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(3)))
                .build();
    }
}

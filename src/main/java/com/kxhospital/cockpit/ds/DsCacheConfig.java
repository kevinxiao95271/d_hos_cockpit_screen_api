package com.kxhospital.cockpit.ds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableCaching
public class DsCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        log.info(">>> 使用本地内存缓存 (ConcurrentMap)");
        return new ConcurrentMapCacheManager(
            "dsRegions", "dsTasks", "dsStatTrend", "dsStatSummary",
            "dsProvince", "dsCity", "dsOrgs",
            "dsQcbProvince", "dsQcbCity", "dsQcbOrgs"
        );
    }
}

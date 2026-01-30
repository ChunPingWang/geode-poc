package com.example.geodedemo.config;

import com.example.geodedemo.entity.Account;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableCachingDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnableLogging;

@Configuration
@ClientCacheApplication(name = "GeodeDemoApp", logLevel = "info")
@EnableLogging(logLevel = "info")
@EnableCachingDefinedRegions(clientRegionShortcut = ClientRegionShortcut.PROXY)
public class GeodeConfig {

    @Bean
    public Region<String, Account> accountRegion(GemFireCache cache) {
        return cache.getRegion("Accounts");
    }
}

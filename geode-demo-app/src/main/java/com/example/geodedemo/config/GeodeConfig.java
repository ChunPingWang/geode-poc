package com.example.geodedemo.config;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableCachingDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnableLogging;

@Configuration
@ClientCacheApplication(name = "GeodeDemoApp", logLevel = "info")
@EnableLogging(logLevel = "info")
@EnableCachingDefinedRegions(clientRegionShortcut = ClientRegionShortcut.PROXY)
public class GeodeConfig {

    // Additional Geode configuration can be added here
    // For example, custom region configurations, PDX serializers, etc.
}

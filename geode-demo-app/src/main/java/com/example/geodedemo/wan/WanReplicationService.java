package com.example.geodedemo.wan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Service for WAN replication information and operations.
 *
 * Note: Full WAN management requires server-side access via JMX.
 * This service provides client-side information and test operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WanReplicationService {

    private final GemFireCache cache;

    /**
     * Get basic WAN replication information available from client.
     */
    public WanReplicationInfo getWanInfo() {
        WanReplicationInfo.WanReplicationInfoBuilder builder = WanReplicationInfo.builder();

        try {
            // Get distributed system properties
            Properties props = cache.getDistributedSystem().getProperties();

            String dsId = props.getProperty("distributed-system-id", "unknown");
            String remoteLocators = props.getProperty("remote-locators", "none");

            builder.distributedSystemId(parseDistributedSystemId(dsId));
            builder.remoteLocators(parseRemoteLocators(remoteLocators));

            // Gateway sender/receiver info requires JMX access
            // For client-side, we provide placeholder info
            builder.gatewaySenders(new ArrayList<>());
            builder.gatewayReceivers(new ArrayList<>());

            log.info("WAN Info - DS ID: {}, Remote Locators: {}", dsId, remoteLocators);

        } catch (Exception e) {
            log.warn("Could not retrieve full WAN info: {}", e.getMessage());
        }

        return builder.build();
    }

    /**
     * Test WAN replication by writing and reading from regions.
     */
    public WanTestResult testWanReplication(String testKey, String testValue) {
        WanTestResult result = new WanTestResult();
        result.setTestKey(testKey);
        result.setTestValue(testValue);

        try {
            Region<String, String> customersRegion = cache.getRegion("Customers");

            if (customersRegion == null) {
                result.setSuccess(false);
                result.setMessage("Customers region not found");
                return result;
            }

            // Write test data
            long startTime = System.currentTimeMillis();
            customersRegion.put(testKey, testValue);
            long writeTime = System.currentTimeMillis() - startTime;

            // Read back to verify
            startTime = System.currentTimeMillis();
            String readValue = customersRegion.get(testKey);
            long readTime = System.currentTimeMillis() - startTime;

            result.setSuccess(testValue.equals(readValue));
            result.setWriteTimeMs(writeTime);
            result.setReadTimeMs(readTime);
            result.setMessage("Data written successfully. Check remote site for replication.");

            log.info("WAN test - Key: {}, Write: {}ms, Read: {}ms", testKey, writeTime, readTime);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("WAN test failed: " + e.getMessage());
            log.error("WAN test failed", e);
        }

        return result;
    }

    /**
     * Get pool information for WAN connectivity.
     */
    public List<PoolInfo> getPoolInfo() {
        List<PoolInfo> pools = new ArrayList<>();

        try {
            for (Pool pool : PoolManager.getAll().values()) {
                PoolInfo info = new PoolInfo();
                info.setName(pool.getName());
                info.setLocators(pool.getLocators().toString());
                info.setServers(pool.getServers().toString());
                info.setConnected(!pool.isDestroyed());
                info.setMinConnections(pool.getMinConnections());
                info.setMaxConnections(pool.getMaxConnections());
                pools.add(info);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve pool info: {}", e.getMessage());
        }

        return pools;
    }

    /**
     * Get region names that may have WAN gateway senders attached.
     */
    public List<String> getWanEnabledRegions() {
        List<String> regions = new ArrayList<>();

        try {
            Set<Region<?, ?>> rootRegions = cache.rootRegions();
            for (Region<?, ?> region : rootRegions) {
                regions.add(region.getName());
            }
        } catch (Exception e) {
            log.warn("Could not retrieve regions: {}", e.getMessage());
        }

        return regions;
    }

    private Integer parseDistributedSystemId(String dsId) {
        try {
            return Integer.parseInt(dsId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> parseRemoteLocators(String remoteLocators) {
        List<String> result = new ArrayList<>();
        if (remoteLocators != null && !remoteLocators.equals("none") && !remoteLocators.isEmpty()) {
            for (String locator : remoteLocators.split(",")) {
                result.add(locator.trim());
            }
        }
        return result;
    }

    // Inner classes for result types

    @lombok.Data
    public static class WanTestResult {
        private String testKey;
        private String testValue;
        private boolean success;
        private String message;
        private Long writeTimeMs;
        private Long readTimeMs;
    }

    @lombok.Data
    public static class PoolInfo {
        private String name;
        private String locators;
        private String servers;
        private boolean connected;
        private Integer minConnections;
        private Integer maxConnections;
    }
}

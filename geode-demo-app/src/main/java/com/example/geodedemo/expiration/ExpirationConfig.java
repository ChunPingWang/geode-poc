package com.example.geodedemo.expiration;

import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.CustomExpiry;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.Region;

/**
 * Expiration Configuration for Geode regions.
 *
 * Geode supports two types of expiration:
 * 1. Time-To-Live (TTL) - Time since entry was created/updated
 * 2. Idle Timeout - Time since entry was last accessed
 *
 * Expiration Actions:
 * - DESTROY: Remove the entry from the region
 * - INVALIDATE: Set value to null but keep the key
 * - LOCAL_DESTROY: Remove only from local cache (for replicated regions)
 * - LOCAL_INVALIDATE: Invalidate only in local cache
 *
 * Configuration is typically done via gfsh or XML, but can also be programmatic.
 */
@Slf4j
public class ExpirationConfig {

    /**
     * Custom expiry for session data - expires after 30 minutes of inactivity.
     */
    public static class SessionExpiry<K, V> implements CustomExpiry<K, V> {

        private static final int SESSION_TIMEOUT_SECONDS = 1800; // 30 minutes

        @Override
        public ExpirationAttributes getExpiry(Region.Entry<K, V> entry) {
            // Can implement dynamic expiration based on entry data
            return new ExpirationAttributes(SESSION_TIMEOUT_SECONDS, ExpirationAction.DESTROY);
        }

        @Override
        public void close() {
            log.info("Session expiry policy closed");
        }
    }

    /**
     * Custom expiry for cache entries - TTL based on data type.
     */
    public static class DataTypeExpiry<K, V> implements CustomExpiry<K, V> {

        private static final int DEFAULT_TTL = 3600; // 1 hour
        private static final int VOLATILE_TTL = 300; // 5 minutes
        private static final int PERMANENT_TTL = 86400; // 24 hours

        @Override
        public ExpirationAttributes getExpiry(Region.Entry<K, V> entry) {
            V value = entry.getValue();

            // Example: Different TTL based on object type or properties
            int ttl = DEFAULT_TTL;

            if (value != null) {
                String className = value.getClass().getSimpleName();
                switch (className) {
                    case "SessionData":
                        ttl = VOLATILE_TTL;
                        break;
                    case "UserPreferences":
                        ttl = PERMANENT_TTL;
                        break;
                    default:
                        ttl = DEFAULT_TTL;
                }
            }

            return new ExpirationAttributes(ttl, ExpirationAction.DESTROY);
        }

        @Override
        public void close() {
            log.info("Data type expiry policy closed");
        }
    }

    /**
     * Get standard TTL expiration attributes.
     */
    public static ExpirationAttributes getTTLExpiration(int seconds) {
        return new ExpirationAttributes(seconds, ExpirationAction.DESTROY);
    }

    /**
     * Get idle timeout expiration attributes.
     */
    public static ExpirationAttributes getIdleExpiration(int seconds) {
        return new ExpirationAttributes(seconds, ExpirationAction.INVALIDATE);
    }

    /**
     * Example gfsh commands for configuring expiration:
     *
     * # Create region with TTL (entries expire 1 hour after creation)
     * gfsh> create region --name=Sessions --type=PARTITION
     *       --entry-time-to-live-expiration=3600
     *       --entry-time-to-live-expiration-action=DESTROY
     *
     * # Create region with idle timeout (entries expire after 30 min of no access)
     * gfsh> create region --name=Cache --type=PARTITION
     *       --entry-idle-time-expiration=1800
     *       --entry-idle-time-expiration-action=INVALIDATE
     *
     * # Region-level expiration (entire region expires)
     * gfsh> create region --name=TempData --type=LOCAL
     *       --region-time-to-live-expiration=7200
     *       --region-time-to-live-expiration-action=DESTROY
     */
}

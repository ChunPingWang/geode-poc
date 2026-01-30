package com.example.geodedemo.compression;

import lombok.extern.slf4j.Slf4j;

/**
 * Compression Configuration for Geode regions.
 *
 * Geode supports compression to reduce memory usage and network transfer size.
 *
 * Supported Compressors:
 * - Snappy (default, fast compression)
 * - LZ4 (very fast, moderate compression)
 * - Custom compressors via Compressor interface
 *
 * Benefits:
 * - Reduced memory footprint
 * - Faster WAN replication
 * - Lower disk storage for persistent regions
 *
 * Trade-offs:
 * - CPU overhead for compression/decompression
 * - Best for large objects with compressible data
 *
 * Configuration via gfsh:
 *
 * # Enable Snappy compression (requires snappy-java in classpath)
 * gfsh> create region --name=CompressedData --type=PARTITION
 *       --compressor=org.apache.geode.compression.SnappyCompressor
 *
 * # For disk stores with compression
 * gfsh> create disk-store --name=compressedDiskStore
 *       --dir=/data/geode/compressed
 *       --allow-force-compaction=true
 *
 * gfsh> create region --name=PersistentCompressed
 *       --type=PARTITION_REDUNDANT_PERSISTENT
 *       --disk-store=compressedDiskStore
 *       --compressor=org.apache.geode.compression.SnappyCompressor
 */
@Slf4j
public class CompressionConfig {

    /**
     * Snappy Compressor class name.
     * Requires snappy-java dependency.
     */
    public static final String SNAPPY_COMPRESSOR =
        "org.apache.geode.compression.SnappyCompressor";

    /**
     * Compression recommendations based on data characteristics:
     *
     * | Data Type          | Compression | Reason                              |
     * |--------------------|-------------|-------------------------------------|
     * | JSON/XML strings   | High        | Very compressible text data         |
     * | Binary blobs       | Medium      | Depends on content                  |
     * | Small objects <1KB | Low/None    | Overhead may exceed benefit         |
     * | Already compressed | None        | Images, videos already compressed   |
     * | Numeric data       | Medium      | Moderate compression ratio          |
     *
     * Performance considerations:
     * - Compression adds ~5-15% CPU overhead
     * - Memory savings typically 40-70% for text data
     * - Network transfer reduction improves WAN replication
     */

    /**
     * Example region attributes for compressed region:
     *
     * <region-attributes>
     *   <compressor>
     *     <class-name>org.apache.geode.compression.SnappyCompressor</class-name>
     *   </compressor>
     * </region-attributes>
     */

    /**
     * Programmatic compression setup example:
     *
     * RegionFactory<String, byte[]> factory = cache.createRegionFactory(PARTITION);
     * factory.setCompressor(SnappyCompressor.getDefaultInstance());
     * Region<String, byte[]> region = factory.create("CompressedRegion");
     */

    public static void logCompressionInfo() {
        log.info("Compression configuration:");
        log.info("  - Snappy compressor: {}", SNAPPY_COMPRESSOR);
        log.info("  - Enable via gfsh: create region --compressor={}",
            SNAPPY_COMPRESSOR);
    }
}

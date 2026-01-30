package com.example.geodedemo.wan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Information about WAN replication configuration and status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WanReplicationInfo {

    private Integer distributedSystemId;
    private List<String> remoteLocators;
    private List<GatewaySenderInfo> gatewaySenders;
    private List<GatewayReceiverInfo> gatewayReceivers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewaySenderInfo {
        private String id;
        private Integer remoteDistributedSystemId;
        private Boolean running;
        private Boolean paused;
        private Boolean parallel;
        private Integer batchSize;
        private Long batchTimeInterval;
        private Boolean persistenceEnabled;
        private Integer queueSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewayReceiverInfo {
        private String host;
        private Integer port;
        private Boolean running;
        private String bindAddress;
        private Integer startPort;
        private Integer endPort;
    }
}

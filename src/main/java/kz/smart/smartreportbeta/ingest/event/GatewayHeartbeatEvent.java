package kz.smart.smartreportbeta.ingest.event;

import java.time.Instant;

public record GatewayHeartbeatEvent(
        String imei,
        String firmware,
        Double batteryV,
        Double inputV,
        Instant gatewayRtcUtc,
        Instant ingestTs
) {}
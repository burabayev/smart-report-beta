package kz.smart.smartreportbeta.ingest.model;

import java.time.Instant;

public record GatewaySummary(
        String imei,
        String firmware,
        Instant rtcUtc,
        Double batteryV,
        Double inputV,
        Integer lastPacketIndex,
        Instant lastIngestTs
) {
}

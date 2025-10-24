package kz.smart.smartreportbeta.inventory;

import java.time.OffsetDateTime;

public record GatewayDto(
        String imei,
        String firmware,
        OffsetDateTime lastSeen,
        Double lastBattV,
        Double lastInputV,
        String state
) {}

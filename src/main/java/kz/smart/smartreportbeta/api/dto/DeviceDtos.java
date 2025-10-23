package kz.smart.smartreportbeta.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class DeviceDtos {
    public record DeviceListItem(
            String deviceId,
            String model,
            OffsetDateTime lastSeen,
            String lastGatewayImei,
            Integer lastRssi,
            Double lastBattV,
            Double lastTempC,
            Double lastHumidityPct,
            String state
    ) {}

    public record DeviceDetails(
            String deviceId,
            String model,
            OffsetDateTime firstSeen,
            OffsetDateTime lastSeen,
            String lastGatewayImei,
            Integer lastRssi,
            Double lastBattV,
            Double lastTempC,
            Double lastHumidityPct,
            String state
    ) {}

    public record SeriesPoint(
            OffsetDateTime bucket,
            Double avgTempC,
            Double avgHumidityPct,
            Double minTempC,
            Double maxTempC
    ) {}

    public record DeviceListResponse(
            List<DeviceListItem> items,
            int page,
            int size
    ) {}
}

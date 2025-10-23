package kz.smart.smartreportbeta.ingest.model;

import kz.smart.smartreportbeta.protocol.model.SensorType;

import java.time.Instant;

public record ReadingRecord(
        String deviceId,
        SensorType type,
        Double temperatureC,
        Double humidityPercent,   // null если нет
        double batteryV,
        int rssiDbm,
        Instant sensorTsUtc,
        Instant ingestTsUtc,
        String gatewayImei
) {
}

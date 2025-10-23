package kz.smart.smartreportbeta.events;

import kz.smart.smartreportbeta.protocol.model.SensorType;

import java.time.Instant;

public record ReadingEvent(
        String deviceId,
        String gatewayImei,
        SensorType type,
        Instant sensorTs,   // UTC
        Instant ingestTs,   // UTC
        Double temperatureC,
        Double humidityPercent,
        double batteryV,
        int rssiDbm
) {}

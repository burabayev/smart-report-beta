package kz.smart.smartreportbeta.protocol.model;

import java.time.Instant;

public record SensorReading(
        SensorType type,
        String idHex,            // 8 символов, например "08250009"
        boolean batteryLow,      // статусовый бит7
        boolean temperatureAlert,// бит6
        boolean buttonPressed,   // бит5
        boolean ackRequired,     // бит4
        boolean rtcMark,         // бит3
        Double temperatureC,     // null, если «abnormal»
        Double humidityPercent,  // TAG08B: может быть 0.1% шаг; TAG08: FF = нет влажности → null
        double batteryV,         // ед. 1 mV
        int rssiDbm,             // отрицательное значение
        Instant sensorRtcUtc     // метка времени приёма на шлюзе (UTC)
) {
}

package kz.smart.smartreportbeta.protocol.model;

import java.time.Instant;
import java.util.List;

public record Rd07Packet(
        String imei,
        String firmware,                 // например 3.13
        Instant gatewayRtcUtc,           // время шлюза (UTC)
        GatewayStatus status,            // может быть null, если в пакете нет статуса
        List<SensorReading> sensors,     // 0..N записей
        int packetIndex,                 // 1..9999
        int crcGiven,                    // из пакета (MSB first)
        int crcCalculated,               // по правилу протокола
        boolean crcOk
) {
}

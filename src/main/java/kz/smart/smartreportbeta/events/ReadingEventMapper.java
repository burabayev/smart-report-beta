package kz.smart.smartreportbeta.events;

import kz.smart.smartreportbeta.protocol.model.Rd07Packet;
import kz.smart.smartreportbeta.protocol.model.SensorReading;

import java.time.Instant;

public final class ReadingEventMapper {
    private ReadingEventMapper(){}

    public static ReadingEvent from(Rd07Packet p, SensorReading s, Instant ingestTs) {
        return new ReadingEvent(
                s.idHex(),
                p.imei(),
                s.type(),
                s.sensorRtcUtc(),
                ingestTs,
                s.temperatureC(),
                s.humidityPercent(),
                s.batteryV(),
                s.rssiDbm()
        );
    }
}
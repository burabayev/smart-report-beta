package kz.smart.smartreportbeta.ingest.store;

import kz.smart.smartreportbeta.ingest.event.PacketParsedEvent;
import kz.smart.smartreportbeta.ingest.model.GatewaySummary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryGatewayState {
    private static final class State {
        String imei;
        String firmware;
        Instant rtcUtc;
        Double batteryV;
        Double inputV;
        Integer lastPacketIndex;
        Instant lastIngestTs;
    }

    private final Map<String, State> map = new ConcurrentHashMap<>();

    @EventListener
    public void onPacket(PacketParsedEvent ev) {
        var p = ev.packet();
        var s = map.computeIfAbsent(p.imei(), k -> new State());
        s.imei = p.imei();
        s.firmware = p.firmware();
        s.rtcUtc = p.gatewayRtcUtc();
        if (p.status() != null) {
            s.batteryV = p.status().batteryVoltageV();
            s.inputV   = p.status().inputVoltageV();
        }
        s.lastPacketIndex = p.packetIndex();
        s.lastIngestTs = ev.ingestTs();
    }

    public Collection<GatewaySummary> all() {
        return map.values().stream().map(s ->
                new GatewaySummary(s.imei, s.firmware, s.rtcUtc, s.batteryV, s.inputV, s.lastPacketIndex, s.lastIngestTs)
        ).toList();
    }
}

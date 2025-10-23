package kz.smart.smartreportbeta.ingest.store;

import kz.smart.smartreportbeta.ingest.event.PacketParsedEvent;
import kz.smart.smartreportbeta.ingest.model.ReadingRecord;
import kz.smart.smartreportbeta.protocol.model.SensorReading;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryReadingStore {

    private final InMemoryStoreProperties props;

    // deviceId -> deque (молодые справа)
    private final Map<String, Deque<ReadingRecord>> byDevice = new ConcurrentHashMap<>();

    public InMemoryReadingStore(InMemoryStoreProperties props) {
        this.props = props;
    }

    @EventListener
    public void onPacket(PacketParsedEvent ev) {
        var p = ev.packet();
        var ingestTs = ev.ingestTs();
        for (SensorReading s : p.sensors()) {
            var r = new ReadingRecord(
                    s.idHex(), s.type(), s.temperatureC(), s.humidityPercent(),
                    s.batteryV(), s.rssiDbm(), s.sensorRtcUtc(), ingestTs, p.imei()
            );
            add(r);
        }
    }

    private void add(ReadingRecord r) {
        var dq = byDevice.computeIfAbsent(r.deviceId(), k -> new ArrayDeque<>());
        synchronized (dq) {
            dq.addLast(r);
            // простой дедуп по соседней точке: если та же метка и значения — убираем предыдущую
            if (dq.size() >= 2) {
                var last = dq.peekLast();
                var prev = dq.size() >= 2 ? dq.stream().skip(dq.size()-2).findFirst().orElse(null) : null;
                if (prev != null && Objects.equals(prev.sensorTsUtc(), last.sensorTsUtc())
                        && Objects.equals(prev.temperatureC(), last.temperatureC())
                        && Objects.equals(prev.humidityPercent(), last.humidityPercent())) {
                    dq.removeLast(); dq.removeLast(); dq.addLast(last); // оставим одну
                }
            }
            while (dq.size() > props.getPerDeviceLimit()) dq.removeFirst();
        }
    }

    public Optional<ReadingRecord> last(String deviceId) {
        var dq = byDevice.get(deviceId);
        if (dq == null) return Optional.empty();
        synchronized (dq) {
            return Optional.ofNullable(dq.peekLast());
        }
    }

    public List<ReadingRecord> range(String deviceId, Instant from, Instant to, int limit) {
        var dq = byDevice.get(deviceId);
        if (dq == null) return List.of();
        List<ReadingRecord> out = new ArrayList<>();
        synchronized (dq) {
            for (var it : dq) {
                if ((from == null || !it.sensorTsUtc().isBefore(from)) &&
                        (to   == null || !it.sensorTsUtc().isAfter(to))) {
                    out.add(it);
                    if (limit > 0 && out.size() >= limit) break;
                }
            }
        }
        return out;
    }

    public Set<String> deviceIds() { return byDevice.keySet(); }
}

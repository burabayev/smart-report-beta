package kz.smart.smartreportbeta.api;

import kz.smart.smartreportbeta.ingest.model.ReadingRecord;
import kz.smart.smartreportbeta.ingest.store.InMemoryReadingStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class ReadingController {
    private final InMemoryReadingStore store;

    public ReadingController(InMemoryReadingStore store) {
        this.store = store;
    }

    @GetMapping("/readings/last")
    public Optional<ReadingRecord> last(@RequestParam String deviceId) {
        return store.last(deviceId);
    }

    @GetMapping("/readings")
    public List<ReadingRecord> range(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "200") int limit
    ) {
        return store.range(deviceId, from, to, limit);
    }

    @GetMapping("/readings/devices")
    public Set<String> devices() {
        return store.deviceIds();
    }
}
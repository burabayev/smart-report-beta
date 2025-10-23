package kz.smart.smartreportbeta.api;

import kz.smart.smartreportbeta.ingest.store.InMemoryRawFrameStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/debug")
public class DebugController {
    private final InMemoryRawFrameStore raw;

    public DebugController(InMemoryRawFrameStore raw) {
        this.raw = raw;
    }

    @GetMapping("/raw")
    public List<InMemoryRawFrameStore.RawFrameDto> raw(@RequestParam(defaultValue = "50") int limit) {
        return raw.last(limit);
    }
}

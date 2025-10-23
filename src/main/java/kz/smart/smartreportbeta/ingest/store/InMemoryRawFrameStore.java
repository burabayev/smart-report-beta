package kz.smart.smartreportbeta.ingest.store;

import kz.smart.smartreportbeta.ingest.event.RawFrameEvent;
import kz.smart.smartreportbeta.tcp.Hex;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@Component
public class InMemoryRawFrameStore {
    public static record RawFrameDto(String hex, int length, String remote, Instant at) {}

    private final InMemoryStoreProperties props;
    private final ArrayDeque<RawFrameDto> dq = new ArrayDeque<>();

    public InMemoryRawFrameStore(InMemoryStoreProperties props) {
        this.props = props;
    }

    @EventListener
    public void onRaw(RawFrameEvent ev) {
        var hex = Hex.of(ev.frameBodyPlusStop(), 0, ev.frameBodyPlusStop().length);
        var dto = new RawFrameDto(hex, ev.frameBodyPlusStop().length,
                ev.remote()==null ? "-" : ev.remote().toString(), ev.at());
        synchronized (dq) {
            dq.addLast(dto);
            while (dq.size() > props.getRawFramesLimit()) dq.removeFirst();
        }
    }

    public List<RawFrameDto> last(int limit) {
        List<RawFrameDto> out = new ArrayList<>();
        synchronized (dq) {
            var it = dq.descendingIterator();
            while (it.hasNext() && (limit<=0 || out.size()<limit)) out.add(it.next());
        }
        return out;
    }
}

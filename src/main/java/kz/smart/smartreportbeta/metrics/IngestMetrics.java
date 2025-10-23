package kz.smart.smartreportbeta.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import kz.smart.smartreportbeta.ingest.event.AckSentEvent;
import kz.smart.smartreportbeta.ingest.event.PacketParsedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IngestMetrics {
    private final Counter packetsReceived;
    private final Counter readingsParsed;
    private final Counter acksSent;

    public IngestMetrics(MeterRegistry reg) {
        this.packetsReceived = Counter.builder("packets_received_total").register(reg);
        this.readingsParsed  = Counter.builder("readings_parsed_total").register(reg);
        this.acksSent        = Counter.builder("acks_sent_total").register(reg);
    }

    @EventListener
    public void onPacket(PacketParsedEvent ev) {
        packetsReceived.increment();
        readingsParsed.increment(ev.packet().sensors().size());
    }

    @EventListener
    public void onAck(AckSentEvent ev) {
        acksSent.increment();
    }
}

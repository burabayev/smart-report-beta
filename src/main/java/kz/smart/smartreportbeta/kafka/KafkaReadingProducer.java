package kz.smart.smartreportbeta.kafka;

import kz.smart.smartreportbeta.events.ReadingEvent;
import kz.smart.smartreportbeta.events.ReadingEventMapper;
import kz.smart.smartreportbeta.ingest.event.PacketParsedEvent;
import kz.smart.smartreportbeta.protocol.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaReadingProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaReadingProducer.class);

    private final KafkaTemplate<String, ReadingEvent> template;
    private final String topic;

    public KafkaReadingProducer(
            @Qualifier("readingKafkaTemplate") KafkaTemplate<String, ReadingEvent> template,
            @Value("${kafka.topics.parsed-readings}") String topic
    ) {
        this.template = template;
        this.topic = topic;
    }

    @EventListener
    public void onParsed(PacketParsedEvent ev) {
        var p = ev.packet();
        for (SensorReading s : p.sensors()) {
            var e = ReadingEventMapper.from(p, s, ev.ingestTs());
            var key = e.deviceId();
            template.send(topic, key, e).whenComplete((meta, ex) -> {
                if (ex != null) {
                    log.warn("Kafka send failed: deviceId={} err={}", key, ex.toString());
                } else {
                    log.debug("Kafka sent: topic={} part={} offset={} key={}",
                            meta.getRecordMetadata().topic(),
                            meta.getRecordMetadata().partition(),
                            meta.getRecordMetadata().offset(),
                            key);
                }
            });
        }
    }
}

package kz.smart.smartreportbeta.kafka;

import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import kz.smart.smartreportbeta.ingest.event.PacketParsedEvent;
import kz.smart.smartreportbeta.protocol.model.Rd07Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class GatewayHeartbeatProducer {
    private static final Logger log = LoggerFactory.getLogger(GatewayHeartbeatProducer.class);

    private final KafkaTemplate<String, GatewayHeartbeatEvent> template;
    private final String topic;

    public GatewayHeartbeatProducer(
            @Qualifier("heartbeatKafkaTemplate") KafkaTemplate<String, GatewayHeartbeatEvent> template,
            @Value("${kafka.topics.gateway-heartbeats}") String topic
    ) {
        this.template = template;
        this.topic = topic;
    }

    @EventListener
    public void onParsed(PacketParsedEvent ev) {
        Rd07Packet p = ev.packet();
        // шлюз жив, даже если sensors.size()==0 — берём сервисные поля
        var hb = new GatewayHeartbeatEvent(
                p.imei(),
                p.firmware(),
                p.status() != null ? p.status().batteryVoltageV() : null,
                p.status() != null ? p.status().inputVoltageV() : null,
                p.gatewayRtcUtc(),
                ev.ingestTs()
        );
        template.send(topic, hb.imei(), hb).whenComplete((meta, ex) -> {
            if (ex != null) {
                log.warn("Kafka HB send failed: imei={} err={}", hb.imei(), ex.toString());
            } else {
                log.debug("Kafka HB sent: topic={} part={} offset={} key={}",
                        meta.getRecordMetadata().topic(),
                        meta.getRecordMetadata().partition(),
                        meta.getRecordMetadata().offset(),
                        hb.imei());
            }
        });
    }
}

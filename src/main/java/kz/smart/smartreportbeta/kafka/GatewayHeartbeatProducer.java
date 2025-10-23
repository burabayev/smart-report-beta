package kz.smart.smartreportbeta.kafka;

import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import kz.smart.smartreportbeta.ingest.event.PacketParsedEvent;
import kz.smart.smartreportbeta.protocol.model.Rd07Packet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class GatewayHeartbeatProducer {
    private final KafkaTemplate<Object, Object> kafka;
    private final String topic;

    public GatewayHeartbeatProducer(
            KafkaTemplate<Object, Object> kafka,
            @Value("${kafka.topics.gateway-heartbeats}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @EventListener
    public void onParsed(PacketParsedEvent evt) {
        Rd07Packet p = evt.packet();
        if (p == null) return;

        if (p.status() == null) return;

        var hb = new GatewayHeartbeatEvent(
                p.imei(),
                p.firmware(),
                p.status().batteryVoltageV(),
                p.status().inputVoltageV(),
                p.gatewayRtcUtc(),
                evt.ingestTs()
        );

        kafka.send(topic, p.imei(), hb);
    }
}

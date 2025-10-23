package kz.smart.smartreportbeta.kafka;

import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import kz.smart.smartreportbeta.inventory.GatewayHeartbeatJdbcWriter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class GatewayHeartbeatConsumer {

    private final GatewayHeartbeatJdbcWriter writer;

    public GatewayHeartbeatConsumer(GatewayHeartbeatJdbcWriter writer) {
        this.writer = writer;
    }

    @KafkaListener(
            topics = "${kafka.topics.gateway-heartbeats}",
            containerFactory = "gatewayHeartbeatListenerContainerFactory",
            groupId = "${kafka.consumer.groups.inventory:smart-report-beta-inventory}"
    )
    public void onMessage(GatewayHeartbeatEvent e) {
        writer.upsert(e);
    }
}

package kz.smart.smartreportbeta.config;

import kz.smart.smartreportbeta.events.ReadingEvent;
import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    // для отправки чтений сенсоров
    @Bean(name = "readingKafkaTemplate")
    public KafkaTemplate<String, ReadingEvent> readingKafkaTemplate(ProducerFactory<String, ReadingEvent> pf) {
        KafkaTemplate<String, ReadingEvent> kt = new KafkaTemplate<>(pf);
        kt.setObservationEnabled(true);
        return kt;
    }

    // для отправки heartbeat'ов шлюза
    @Bean(name = "heartbeatKafkaTemplate")
    public KafkaTemplate<String, GatewayHeartbeatEvent> heartbeatKafkaTemplate(ProducerFactory<String, GatewayHeartbeatEvent> pf) {
        KafkaTemplate<String, GatewayHeartbeatEvent> kt = new KafkaTemplate<>(pf);
        kt.setObservationEnabled(true);
        return kt;
    }
}

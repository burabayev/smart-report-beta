package kz.smart.smartreportbeta.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Единственный KafkaTemplate (generic) для всех продьюсеров.
 * ProducerFactory берём из автоконфигурации Spring Boot (spring.kafka.producer.* в application.yaml).
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> pf) {
        KafkaTemplate<Object, Object> kt = new KafkaTemplate<>(pf);
        kt.setObservationEnabled(true); // метрики Micrometer
        return kt;
    }

    /**
     * Топик для heartbeat'ов шлюза (compact, ключ = IMEI).
     * Следи, чтобы этот бин не дублировался в других конфигурациях.
     */
    @Bean
    public NewTopic gatewayHeartbeatsTopic(@Value("${kafka.topics.gateway-heartbeats}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }
}

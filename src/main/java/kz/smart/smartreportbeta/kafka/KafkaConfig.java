package kz.smart.smartreportbeta.kafka;

import kz.smart.smartreportbeta.events.ReadingEvent;
import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrap;
    @Value("${spring.kafka.consumer.group-id:smart-report-beta-writer}")
    String readingsGroupId;

    // --------- Listener factory для ReadingEvent (parsed-readings) ----------
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReadingEvent> kafkaListenerContainerFactory() {
        JsonDeserializer<ReadingEvent> valueDeserializer = new JsonDeserializer<>(ReadingEvent.class, false);
        valueDeserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, readingsGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, ReadingEvent>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer));

        var errorHandler = new DefaultErrorHandler(new FixedBackOff(0L, 0L));
        errorHandler.addNotRetryableExceptions(BadSqlGrammarException.class, DataIntegrityViolationException.class);
        factory.setCommonErrorHandler(errorHandler);

        factory.setConcurrency(3);
        return factory;
    }

    // --------- Listener factory для GatewayHeartbeatEvent (gateway-heartbeats) ----------
    @Bean(name = "gatewayHeartbeatListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, GatewayHeartbeatEvent> gatewayHeartbeatListenerContainerFactory(
            @Value("${kafka.consumer.groups.inventory:smart-report-beta-inventory}") String groupId
    ) {
        JsonDeserializer<GatewayHeartbeatEvent> valueDeserializer =
                new JsonDeserializer<>(GatewayHeartbeatEvent.class, false);
        valueDeserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, GatewayHeartbeatEvent>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer));

        var errorHandler = new DefaultErrorHandler(new FixedBackOff(0L, 0L));
        errorHandler.addNotRetryableExceptions(BadSqlGrammarException.class, DataIntegrityViolationException.class);
        factory.setCommonErrorHandler(errorHandler);

        factory.setConcurrency(1); // нам достаточно одной партиции и одного потока
        return factory;
    }

    // --------- Топики ----------
    @Bean
    public NewTopic parsedReadingsTopic(@Value("${kafka.topics.parsed-readings}") String topic) {
        return TopicBuilder.name(topic).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic gatewayHeartbeatsTopic(@Value("${kafka.topics.gateway-heartbeats}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .config("cleanup.policy", "compact")
                .build();
    }
}

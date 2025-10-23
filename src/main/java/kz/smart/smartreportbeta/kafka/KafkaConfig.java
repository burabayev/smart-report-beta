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
    String consumerGroupId;

    @Bean(name = "hbKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, GatewayHeartbeatEvent> hbKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap) {

        JsonDeserializer<GatewayHeartbeatEvent> valueDeserializer =
                new JsonDeserializer<>(GatewayHeartbeatEvent.class, false);
        valueDeserializer.addTrustedPackages("*");

        var props = new java.util.HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, GatewayHeartbeatEvent>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer));
        factory.setConcurrency(1); // heartbeat-топик compact, одной партиции достаточно
        return factory;
    }

    /**
     * Фабрика контейнеров для @KafkaListener (консюмер ReadingEvent).
     * Продьюсерские бины здесь НЕ объявляем, чтобы не конфликтовать.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReadingEvent> kafkaListenerContainerFactory() {
        // Строго типизированный десериализатор под ReadingEvent
        JsonDeserializer<ReadingEvent> valueDeserializer = new JsonDeserializer<>(ReadingEvent.class, false);
        valueDeserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, ReadingEvent>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer));

        // Ошибки SQL — не ретраим (чтобы не зациклиться)
        var errorHandler = new DefaultErrorHandler(new FixedBackOff(0L, 0L));
        errorHandler.addNotRetryableExceptions(BadSqlGrammarException.class, DataIntegrityViolationException.class);
        factory.setCommonErrorHandler(errorHandler);

        factory.setConcurrency(3);
        return factory;
    }

    /**
     * Топик с распарсенными показаниями.
     * Если авто-создание на брокере выключено — создастся при старте приложения.
     */
    @Bean
    public NewTopic parsedReadingsTopic(@Value("${kafka.topics.parsed-readings}") String topic) {
        return new NewTopic(topic, 6, (short) 1);
    }
}

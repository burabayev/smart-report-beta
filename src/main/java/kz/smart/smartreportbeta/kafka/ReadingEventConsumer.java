package kz.smart.smartreportbeta.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import kz.smart.smartreportbeta.db.InventoryJdbcWriter;
import kz.smart.smartreportbeta.db.ReadingJdbcWriter;
import kz.smart.smartreportbeta.events.ReadingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ReadingEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReadingEventConsumer.class);

    private final ReadingJdbcWriter readingWriter;
    private final InventoryJdbcWriter inventoryWriter;

    private final Counter dbWritesOk;
    private final Counter dbWritesError;
    private final Timer ingestLatency;

    public ReadingEventConsumer(ReadingJdbcWriter readingWriter,
                                InventoryJdbcWriter inventoryWriter,
                                MeterRegistry registry) {
        this.readingWriter = readingWriter;
        this.inventoryWriter = inventoryWriter;

        this.dbWritesOk = Counter.builder("db_writes_total")
                .tag("status", "ok")
                .description("Successful DB writes (readings+inventory)")
                .register(registry);

        this.dbWritesError = Counter.builder("db_writes_total")
                .tag("status", "error")
                .description("Failed DB writes (readings+inventory)")
                .register(registry);

        this.ingestLatency = Timer.builder("ingest_latency_seconds")
                .description("Sensor-to-ingest latency")
                .publishPercentileHistogram()
                .register(registry);
    }

    @KafkaListener(
            topics = "${kafka.topics.parsed-readings}",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:smart-report-beta-writer}"
    )
    public void onMessage(@Payload ReadingEvent e) {
        // latency: sensor_ts -> ingest_ts (что прислал producer)
        var d = Duration.between(e.sensorTs(), e.ingestTs());
        if (!d.isNegative()) ingestLatency.record(d);

        try {
            readingWriter.upsert(e);      // запись в telemetry.readings
            inventoryWriter.upsertFromEvent(e); // обновление инвентаря
            dbWritesOk.increment();
            log.debug("DB write ok: deviceId={} ts={}", e.deviceId(), e.sensorTs());
        } catch (Exception ex) {
            dbWritesError.increment();
            log.error("DB write error: {}", ex.getMessage(), ex);
            throw ex; // отдаём хэндлеру (у нас настроен no-retry для SQL ошибок)
        }
    }
}

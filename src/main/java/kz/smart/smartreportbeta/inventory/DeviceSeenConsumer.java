package kz.smart.smartreportbeta.inventory;

import kz.smart.smartreportbeta.events.ReadingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class DeviceSeenConsumer {
    private static final Logger log = LoggerFactory.getLogger(DeviceSeenConsumer.class);

    private final JdbcTemplate jdbc;

    public DeviceSeenConsumer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @KafkaListener(
            topics = "${kafka.topics.parsed-readings}",
            groupId = "smart-report-beta-inventory",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReading(ReadingEvent e) {
        // e.deviceId() обязателен, т.к. это показания сенсора (а не heartbeat)
        if (e.deviceId() == null) {
            // никаких апдейтов devices от heartbeat/пустых событий
            return;
        }
        Instant seen = e.ingestTs() != null ? e.ingestTs() : Instant.now();

        jdbc.update("""
            INSERT INTO inventory.devices(device_id, last_seen)
            VALUES (?, ?)
            ON CONFLICT (device_id) DO UPDATE
              SET last_seen = EXCLUDED.last_seen
            """,
                e.deviceId(),
                Timestamp.from(seen)
        );
    }
}

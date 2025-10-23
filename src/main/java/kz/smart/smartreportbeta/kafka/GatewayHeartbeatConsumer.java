package kz.smart.smartreportbeta.kafka;

import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class GatewayHeartbeatConsumer {

    private final JdbcTemplate jdbc;

    public GatewayHeartbeatConsumer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ВАЖНО: указываем свой containerFactory
    @KafkaListener(
            topics = "${kafka.topics.gateway-heartbeats}",
            groupId = "smart-report-beta-inventory",
            containerFactory = "hbKafkaListenerContainerFactory"
    )
    public void onMessage(GatewayHeartbeatEvent hb) {
        // last_seen ставим по времени приёма на сервере (ingestTs),
        // чтобы оживление видно было даже если RTC шлюза отстаёт.
        OffsetDateTime seen = hb.ingestTs() != null
                ? OffsetDateTime.ofInstant(hb.ingestTs(), ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        jdbc.update("""
            INSERT INTO inventory.gateways(imei, last_seen, firmware, last_batt_v, last_input_v)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (imei) DO UPDATE SET
              last_seen   = EXCLUDED.last_seen,
              firmware    = COALESCE(EXCLUDED.firmware, inventory.gateways.firmware),
              last_batt_v = EXCLUDED.last_batt_v,
              last_input_v= EXCLUDED.last_input_v
            """,
                hb.imei(),
                seen,
                hb.firmware(),
                hb.batteryV(),
                hb.inputV()
        );
    }
}
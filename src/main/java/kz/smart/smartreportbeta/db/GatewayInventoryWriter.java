package kz.smart.smartreportbeta.db;

import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class GatewayInventoryWriter {

    private final JdbcTemplate jdbc;

    public GatewayInventoryWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(GatewayHeartbeatEvent hb) {
        jdbc.update("""
            INSERT INTO inventory.gateways(imei, firmware, last_seen, last_batt_v, last_input_v)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (imei) DO UPDATE SET
              -- firmware/напряжения обновляем только если пришли ненулевые значения,
              -- иначе оставляем старые
              firmware     = COALESCE(EXCLUDED.firmware,     inventory.gateways.firmware),
              last_seen    = GREATEST(inventory.gateways.last_seen, EXCLUDED.last_seen),
              last_batt_v  = COALESCE(EXCLUDED.last_batt_v,  inventory.gateways.last_batt_v),
              last_input_v = COALESCE(EXCLUDED.last_input_v, inventory.gateways.last_input_v)
            """,
                hb.imei(),
                hb.firmware(),
                Timestamp.from(hb.ingestTs()),
                hb.batteryV(),
                hb.inputV()
        );
    }
}

package kz.smart.smartreportbeta.db;

import kz.smart.smartreportbeta.config.InventoryProperties;
import kz.smart.smartreportbeta.events.ReadingEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class InventoryJdbcWriter {

    private static final String UPSERT_GATEWAY = """
      INSERT INTO inventory.gateways(imei, last_seen, firmware, last_batt_v, last_input_v)
      VALUES (?, ?, ?, ?, ?)
      ON CONFLICT (imei) DO UPDATE
      SET last_seen = EXCLUDED.last_seen,
          firmware  = COALESCE(EXCLUDED.firmware, inventory.gateways.firmware),
          last_batt_v = EXCLUDED.last_batt_v,
          last_input_v = EXCLUDED.last_input_v
      """;

    private static final String UPSERT_DEVICE = """
      INSERT INTO inventory.devices(
        device_id, 
        model, 
        first_seen, 
        last_seen, 
        last_gateway_imei,
        last_rssi, 
        last_batt_v, 
        last_temp_c, 
        last_humidity_pct, 
        state
      )
      VALUES (?, ?, now(), ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (device_id) DO UPDATE
      SET last_seen         = EXCLUDED.last_seen,
          last_gateway_imei = EXCLUDED.last_gateway_imei,
          last_rssi         = EXCLUDED.last_rssi,
          last_batt_v       = EXCLUDED.last_batt_v,
          last_temp_c       = EXCLUDED.last_temp_c,
          last_humidity_pct = EXCLUDED.last_humidity_pct,
          model             = COALESCE(EXCLUDED.model, inventory.devices.model),
          state             = EXCLUDED.state
      """;

    private final JdbcTemplate jdbc;
    private final InventoryProperties props;

    public InventoryJdbcWriter(JdbcTemplate jdbc, InventoryProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    public void upsertFromEvent(ReadingEvent e) {
        // 1) Обновим шлюз (минимальная инфа из события). Firmware/напряжения шлюза могут быть null — это ок.
        jdbc.update(conn -> {
            var ps = conn.prepareStatement(UPSERT_GATEWAY);
            int i = 1;
            ps.setString(i++, e.gatewayImei());
            ps.setObject(i++, OffsetDateTime.ofInstant(e.ingestTs(), ZoneOffset.UTC));
            ps.setString(i++, null);                // firmware не знаем из ReadingEvent
            ps.setObject(i++, null, Types.DOUBLE);  // last_batt_v шлюза (нет в e)
            ps.setObject(i++, null, Types.DOUBLE);  // last_input_v шлюза (нет в e)
            return ps;
        });

        // 2) Вычислим модель и состояние устройства
        String model = (e.humidityPercent() != null) ? "TZ-Tag08B" : "TZ-Tag08";

        Instant now = Instant.now();
        long ageSec = Math.max(0, now.getEpochSecond() - e.ingestTs().getEpochSecond());
        String state = (ageSec <= props.getOnlineThresholdSeconds()) ? "online" : "stale";

        // 3) Обновим/создадим устройство
        jdbc.update(conn -> {
            var ps = conn.prepareStatement(UPSERT_DEVICE);
            int i = 1;

            ps.setString(i++, e.deviceId());
            ps.setString(i++, model);
            ps.setObject(i++, OffsetDateTime.ofInstant(e.ingestTs(), ZoneOffset.UTC));   // last_seen
            ps.setString(i++, e.gatewayImei());
            ps.setObject(i++, e.rssiDbm(), Types.SMALLINT);
            ps.setDouble(i++, e.batteryV());
            if (e.temperatureC() == null) ps.setNull(i++, Types.DOUBLE); else ps.setDouble(i++, e.temperatureC());
            if (e.humidityPercent() == null) ps.setNull(i++, Types.DOUBLE); else ps.setDouble(i++, e.humidityPercent());
            ps.setString(i++, state);

            return ps;
        });
    }
}

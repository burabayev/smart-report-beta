package kz.smart.smartreportbeta.db;

import kz.smart.smartreportbeta.events.ReadingEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class ReadingJdbcWriter {

    private static final String SQL = """
      INSERT INTO telemetry.readings(
        device_id, 
        gateway_imei, 
        type, 
        sensor_ts, 
        ingest_ts,
        temperature_c, 
        humidity_pct, 
        battery_v, 
        rssi_dbm
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (device_id, sensor_ts) DO NOTHING
      """;

    private final JdbcTemplate jdbc;

    public ReadingJdbcWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(ReadingEvent e) {
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(SQL);
            int i = 1;

            ps.setString(i++, e.deviceId());
            ps.setString(i++, e.gatewayImei());
            ps.setString(i++, e.type().name());

            // ВАЖНО: timestamptz — отдаём как OffsetDateTime в UTC
            ps.setObject(i++, OffsetDateTime.ofInstant(e.sensorTs(), ZoneOffset.UTC));
            ps.setObject(i++, OffsetDateTime.ofInstant(e.ingestTs(), ZoneOffset.UTC));

            if (e.temperatureC() == null) ps.setNull(i++, Types.DOUBLE); else ps.setDouble(i++, e.temperatureC());
            if (e.humidityPercent() == null) ps.setNull(i++, Types.DOUBLE); else ps.setDouble(i++, e.humidityPercent());

            ps.setDouble(i++, e.batteryV());
            ps.setInt(i++, e.rssiDbm());

            return ps;
        });
    }
}

package kz.smart.smartreportbeta.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class DeviceQueryRepository {

    private final JdbcTemplate jdbc;

    public DeviceQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listDevices(String state, String model, String gateway,
                                                 String q, int offset, int limit) {

        var sb = new StringBuilder("""
         SELECT device_id, model, last_seen, last_gateway_imei, last_rssi, last_batt_v, last_temp_c, last_humidity_pct, state
         FROM inventory.devices 
         WHERE 1=1
         """);

        new Object() {
            void add(String s) {
                sb.append(s);
            }
        };

        new Object() {}; // no-op just to keep the code compact

        var args = new java.util.ArrayList<Object>();

        if (state != null && !state.isBlank()) {
            sb.append(" AND state = ?");
            args.add(state);
        }
        if (model != null && !model.isBlank()) {
            sb.append(" AND model = ?");
            args.add(model);
        }
        if (gateway != null && !gateway.isBlank()) {
            sb.append(" AND last_gateway_imei = ?");
            args.add(gateway);
        }
        if (q != null && !q.isBlank()) {
            sb.append(" AND device_id ILIKE ?");
            args.add("%"+q+"%");
        }

        sb.append(" ORDER BY last_seen DESC OFFSET ? LIMIT ?");
        args.add(offset);
        args.add(limit);

        return jdbc.queryForList(sb.toString(), args.toArray());
    }

    public Map<String, Object> getDevice(String deviceId) {
        return jdbc.queryForMap("""
      SELECT device_id, model, first_seen, last_seen, last_gateway_imei, last_rssi, last_batt_v, last_temp_c, last_humidity_pct, state
      FROM inventory.devices
      WHERE device_id = ?
      """, deviceId);
    }

    public List<ReadingPoint> timeSeries(String deviceId, OffsetDateTime from, OffsetDateTime to, String resolution) {
        // Timescale агрегирование
        return jdbc.query("""
        SELECT time_bucket(?::interval, sensor_ts) AS bucket,
               avg(temperature_c) AS avg_temp_c,
               avg(humidity_pct)  AS avg_humidity_pct,
               min(temperature_c) AS min_temp_c,
               max(temperature_c) AS max_temp_c
        FROM telemetry.readings
        WHERE device_id = ?
          AND sensor_ts >= ?
          AND sensor_ts <  ?
        GROUP BY bucket
        ORDER BY bucket
        """,
                ps -> {
                    int i=1;
                    ps.setString(i++, resolution == null || resolution.isBlank() ? "1 minute" : resolution);
                    ps.setString(i++, deviceId);
                    ps.setObject(i++, from);
                    ps.setObject(i++, to);
                },
                (ResultSet rs, int rowNum) -> new ReadingPoint(
                        rs.getObject("bucket", OffsetDateTime.class),
                        (Double) rs.getObject("avg_temp_c"),
                        (Double) rs.getObject("avg_humidity_pct"),
                        (Double) rs.getObject("min_temp_c"),
                        (Double) rs.getObject("max_temp_c")
                )
        );
    }

    public record ReadingPoint(OffsetDateTime bucket,
                               Double avgTempC,
                               Double avgHumidityPct,
                               Double minTempC,
                               Double maxTempC) {}
}

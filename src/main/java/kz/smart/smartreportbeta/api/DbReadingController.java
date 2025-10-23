package kz.smart.smartreportbeta.api;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/db")
public class DbReadingController {
    private final JdbcTemplate jdbc;

    public DbReadingController(JdbcTemplate jdbc){
        this.jdbc=jdbc;
    }

    @GetMapping("/readings/last")
    public Map<String,Object> last(@RequestParam String deviceId) {
        return jdbc.queryForMap("""
        SELECT device_id, gateway_imei, type, sensor_ts, ingest_ts,
               temperature_c, humidity_pct, battery_v, rssi_dbm
          FROM telemetry.readings
         WHERE device_id = ?
      ORDER BY sensor_ts DESC
         LIMIT 1
        """, deviceId);
    }

    @GetMapping("/readings")
    public List<Map<String,Object>> range(
            @RequestParam String deviceId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue="200") int limit
    ){
        String sql = """
        SELECT device_id, gateway_imei, type, sensor_ts, ingest_ts, temperature_c, humidity_pct, battery_v, rssi_dbm
          FROM telemetry.readings
         WHERE device_id = ?
           AND (? IS NULL OR sensor_ts >= ?)
           AND (? IS NULL OR sensor_ts <= ?)
      ORDER BY sensor_ts
         LIMIT ?
        """;
        return jdbc.queryForList(sql, deviceId, from, from, to, to, limit);
    }
}

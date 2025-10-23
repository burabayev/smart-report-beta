package kz.smart.smartreportbeta.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GatewayQueryRepository {
    private final JdbcTemplate jdbc;

    public GatewayQueryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> listGateways() {
        return jdbc.queryForList("""
      SELECT imei, first_seen, last_seen, firmware, last_batt_v, last_input_v
      FROM inventory.gateways
      ORDER BY last_seen DESC
      """);
    }
}

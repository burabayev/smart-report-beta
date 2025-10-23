package kz.smart.smartreportbeta.inventory;

import kz.smart.smartreportbeta.ingest.event.GatewayHeartbeatEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;

@Repository
public class GatewayHeartbeatJdbcWriter {

    private final JdbcTemplate jdbc;

    public GatewayHeartbeatJdbcWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(GatewayHeartbeatEvent e) {
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                INSERT INTO inventory.gateways(imei, firmware, last_seen, last_batt_v, last_input_v)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (imei) DO UPDATE
                SET firmware   = EXCLUDED.firmware,
                    last_seen  = EXCLUDED.last_seen,
                    last_batt_v= EXCLUDED.last_batt_v,
                    last_input_v=EXCLUDED.last_input_v
                """);
            ps.setString(1, e.imei());
            ps.setString(2, e.firmware());
            ps.setTimestamp(3, Timestamp.from(e.ingestTs()));

            if (e.batteryV() == null) ps.setNull(4, Types.DOUBLE); else ps.setDouble(4, e.batteryV());
            if (e.inputV() == null) ps.setNull(5, Types.DOUBLE); else ps.setDouble(5, e.inputV());

            return ps;
        });
    }
}

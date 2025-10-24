package kz.smart.smartreportbeta.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class GatewayJdbcRepository {
    private final JdbcTemplate jdbc;

    public GatewayJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<GatewayDto> findPage(int page, int size) {
        int offset = Math.max(0, page) * Math.max(1, size);
        return jdbc.query("""
                select imei, firmware, last_seen, last_batt_v, last_input_v, state
                from inventory.gateways
                order by last_seen desc
                offset ? limit ?
                """,
                (rs, i) -> new GatewayDto(
                        rs.getString("imei"),
                        rs.getString("firmware"),
                        toOdt(rs.getTimestamp("last_seen")),
                        (Double) rs.getObject("last_batt_v"),
                        (Double) rs.getObject("last_input_v"),
                        rs.getString("state")
                ),
                offset, size
        );
    }

    public long countAll() {
        Long c = jdbc.queryForObject("select count(*) from inventory.gateways", Long.class);
        return c == null ? 0 : c;
    }

    public Optional<GatewayDto> findOne(String imei) {
        var list = jdbc.query("""
                select imei, firmware, last_seen, last_batt_v, last_input_v, state
                from inventory.gateways
                where imei = ?
                """,
                (rs, i) -> new GatewayDto(
                        rs.getString("imei"),
                        rs.getString("firmware"),
                        toOdt(rs.getTimestamp("last_seen")),
                        (Double) rs.getObject("last_batt_v"),
                        (Double) rs.getObject("last_input_v"),
                        rs.getString("state")
                ),
                imei
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private static OffsetDateTime toOdt(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }
}

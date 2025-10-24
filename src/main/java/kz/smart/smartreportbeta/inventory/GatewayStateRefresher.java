package kz.smart.smartreportbeta.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GatewayStateRefresher {
    private final JdbcTemplate jdbc;
    private final int onlineThresholdSeconds;

    public GatewayStateRefresher(JdbcTemplate jdbc, @Value("${inventory.onlineThresholdSeconds:600}") int onlineThresholdSeconds) {
        this.jdbc = jdbc;
        this.onlineThresholdSeconds = onlineThresholdSeconds;
    }

    // каждые 30 сек отмечаем online/stale по last_seen
    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT10S")
    public void refresh() {
        jdbc.update("""
            UPDATE inventory.gateways g
               SET state = CASE
                   WHEN now() - g.last_seen <= make_interval(secs => ?) THEN 'online'
                   ELSE 'stale'
               END
            """, onlineThresholdSeconds);
    }
}

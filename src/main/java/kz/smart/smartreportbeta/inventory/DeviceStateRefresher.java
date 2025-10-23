package kz.smart.smartreportbeta.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DeviceStateRefresher {

    private final JdbcTemplate jdbc;
    private final int onlineThresholdSeconds;

    public DeviceStateRefresher(
            JdbcTemplate jdbc,
            @Value("${inventory.onlineThresholdSeconds:600}") int onlineThresholdSeconds
    ) {
        this.jdbc = jdbc;
        this.onlineThresholdSeconds = onlineThresholdSeconds;
    }

    @Scheduled(fixedDelayString = "PT60S", initialDelayString = "PT15S")
    public void refresh() {
        jdbc.update("""
            UPDATE inventory.devices d
               SET state = CASE
                     WHEN now() - d.last_seen <= (? || ' seconds')::interval THEN 'online'
                     ELSE 'stale'
                   END
            """, onlineThresholdSeconds);
    }
}

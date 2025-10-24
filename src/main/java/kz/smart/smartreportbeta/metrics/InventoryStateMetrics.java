package kz.smart.smartreportbeta.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryStateMetrics {
    public InventoryStateMetrics(JdbcTemplate jdbc, MeterRegistry registry) {
        Gauge.builder("inventory_devices_online",
                        () -> jdbc.queryForObject("select count(*) from inventory.devices where state='online'", Long.class))
                .description("Devices marked online").register(registry);

        Gauge.builder("inventory_gateways_online",
                        () -> jdbc.queryForObject("select count(*) from inventory.gateways where state='online'", Long.class))
                .description("Gateways marked online").register(registry);
    }
}

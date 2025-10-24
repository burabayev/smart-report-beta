// src/main/java/kz/smart/smartreportbeta/metrics/InventoryMetrics.java
package kz.smart.smartreportbeta.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import kz.smart.smartreportbeta.inventory.DeviceSeenConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InventoryMetrics {
    private static final Logger log = LoggerFactory.getLogger(InventoryMetrics.class);
    private final JdbcTemplate jdbc;

    private final AtomicInteger gwOnline  = new AtomicInteger(0);
    private final AtomicInteger gwStale   = new AtomicInteger(0);
    private final AtomicInteger devOnline = new AtomicInteger(0);
    private final AtomicInteger devStale  = new AtomicInteger(0);

    public InventoryMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
        this.jdbc = jdbc;

        Gauge.builder("sr_inventory_gateways_online", gwOnline, AtomicInteger::get)
                .description("Gateways with state=online").register(registry);

        Gauge.builder("sr_inventory_gateways_stale", gwStale, AtomicInteger::get)
                .description("Gateways with state=stale").register(registry);

        Gauge.builder("sr_inventory_devices_online", devOnline, AtomicInteger::get)
                .description("Devices with state=online").register(registry);

        Gauge.builder("sr_inventory_devices_stale", devStale, AtomicInteger::get)
                .description("Devices with state=stale").register(registry);
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT10S")
    public void refresh() {
        Integer go = jdbc.queryForObject("select count(*) from inventory.gateways where state = 'online'", Integer.class);
        Integer gs = jdbc.queryForObject("select count(*) from inventory.gateways where state = 'stale'", Integer.class);
        Integer do_ = jdbc.queryForObject("select count(*) from inventory.devices where state = 'online'", Integer.class);
        Integer ds = jdbc.queryForObject("select count(*) from inventory.devices where state = 'stale'", Integer.class);

        gwOnline.set(go == null ? 0 : go);
        gwStale.set(gs == null ? 0 : gs);
        devOnline.set(do_ == null ? 0 : do_);
        devStale.set(ds == null ? 0 : ds);

        log.debug("metrics refresh: gw[online={}, stale={}]; dev[online={}, stale={}]", go, gs, do_, ds);
    }
}

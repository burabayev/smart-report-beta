// src/main/java/kz/smart/smartreportbeta/inventory/OverviewController.java
package kz.smart.smartreportbeta.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OverviewController {
    private final JdbcTemplate jdbc;

    @GetMapping("/api/v1/overview")
    public OverviewDto overview() {
        int gwOnline = jdbc.queryForObject("select count(*) from inventory.gateways where state = 'online'", Integer.class);
        int gwStale  = jdbc.queryForObject("select count(*) from inventory.gateways where state = 'stale'", Integer.class);
        int devOnline= jdbc.queryForObject("select count(*) from inventory.devices where state = 'online'", Integer.class);
        int devStale = jdbc.queryForObject("select count(*) from inventory.devices where state = 'stale'", Integer.class);
        return new OverviewDto(gwOnline, gwStale, devOnline, devStale);
    }

    public record OverviewDto(
            int gatewaysOnline,
            int gatewaysStale,
            int devicesOnline,
            int devicesStale
    ) {}
}

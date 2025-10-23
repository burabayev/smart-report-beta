package kz.smart.smartreportbeta.api;

import kz.smart.smartreportbeta.api.dto.DeviceDtos.*;
import kz.smart.smartreportbeta.db.DeviceQueryRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceQueryRepository repo;

    public DeviceController(DeviceQueryRepository repo) {
        this.repo = repo;
    }

    private static OffsetDateTime toODT(Object v) {
        if (v == null) return null;
        if (v instanceof OffsetDateTime odt) return odt;
        if (v instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        if (v instanceof LocalDateTime ldt) return ldt.atOffset(ZoneOffset.UTC);
        if (v instanceof Date d) return d.toInstant().atOffset(ZoneOffset.UTC);
        throw new IllegalArgumentException("Unsupported temporal type: " + v.getClass());
    }

    private static Integer toInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Short s) return (int) s;
        if (v instanceof Long l) return l.intValue();
        if (v instanceof Number n) return n.intValue();
        throw new IllegalArgumentException("Unsupported numeric type: " + v.getClass());
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Double d) return d;
        if (v instanceof Float f) return (double) f;
        if (v instanceof Number n) return n.doubleValue();
        throw new IllegalArgumentException("Unsupported numeric type: " + v.getClass());
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }

    @GetMapping
    public DeviceListResponse list(@RequestParam(required = false) String state,
                                   @RequestParam(required = false) String model,
                                   @RequestParam(required = false, name="gateway") String gatewayImei,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size
    ) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 0);
        int offset   = safePage * safeSize;

        var rows = repo.listDevices(state, model, gatewayImei, q, offset, safeSize);

        var items = rows.stream().map(m -> new DeviceListItem(
                str(m.get("device_id")),
                str(m.get("model")),
                toODT(m.get("last_seen")),
                str(m.get("last_gateway_imei")),
                toInteger(m.get("last_rssi")),
                toDouble(m.get("last_batt_v")),
                toDouble(m.get("last_temp_c")),
                toDouble(m.get("last_humidity_pct")),
                str(m.get("state"))
        )).toList();

        return new DeviceListResponse(items, safePage, safeSize);
    }

    @GetMapping("/{id}")
    public DeviceDetails get(@PathVariable("id") String deviceId) {
        Map<String, Object> m = repo.getDevice(deviceId);
        return new DeviceDetails(
                str(m.get("device_id")),
                str(m.get("model")),
                toODT(m.get("first_seen")),
                toODT(m.get("last_seen")),
                str(m.get("last_gateway_imei")),
                toInteger(m.get("last_rssi")),
                toDouble(m.get("last_batt_v")),
                toDouble(m.get("last_temp_c")),
                toDouble(m.get("last_humidity_pct")),
                str(m.get("state"))
        );
    }

    @GetMapping("/{id}/readings")
    public List<SeriesPoint> readings(@PathVariable("id") String deviceId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                      @RequestParam(required = false, defaultValue = "1 minute") String resolution
    ) {
        return repo.timeSeries(deviceId, from, to, resolution).stream()
                .map(p -> new SeriesPoint(p.bucket(), p.avgTempC(), p.avgHumidityPct(), p.minTempC(), p.maxTempC()))
                .toList();
    }
}

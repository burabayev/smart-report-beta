// src/main/java/kz/smart/smartreportbeta/inventory/GatewayController.java
package kz.smart.smartreportbeta.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateways")
public class GatewayController {
    private final GatewayJdbcRepository repo;

    public GatewayController(GatewayJdbcRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<Page<GatewayDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<GatewayDto> items = repo.findPage(page, size);
        long total = repo.countAll();
        return ResponseEntity.ok(new Page<>(page, size, total, items));
    }

    @GetMapping("/{imei}")
    public ResponseEntity<GatewayDto> get(@PathVariable String imei) {
        return repo.findOne(imei).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Простой DTO-обёртка для пагинации
    public record Page<T>(int page, int size, long total, List<T> items) {}
}

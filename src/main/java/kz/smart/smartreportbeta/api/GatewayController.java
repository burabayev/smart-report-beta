package kz.smart.smartreportbeta.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import kz.smart.smartreportbeta.db.GatewayQueryRepository;

import java.util.List;
import java.util.Map;

@RestController
public class GatewayController {

    private final GatewayQueryRepository repo;

    public GatewayController(GatewayQueryRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/v1/gateways")
    public List<Map<String, Object>> list() {
        return repo.listGateways();
    }
}

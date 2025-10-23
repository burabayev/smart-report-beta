package kz.smart.smartreportbeta.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI api() {
        return new OpenAPI().info(
                new Info()
                        .title("smart-report-beta API")
                        .version("v0.1")
                        .description("In-memory API для чтения показаний и статуса шлюзов (Phase 3)")
        );
    }
}

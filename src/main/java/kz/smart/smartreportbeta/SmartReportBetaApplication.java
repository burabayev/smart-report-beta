package kz.smart.smartreportbeta;

import kz.smart.smartreportbeta.config.InventoryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
//@EnableConfigurationProperties(InventoryProperties.class)
public class SmartReportBetaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartReportBetaApplication.class, args);
    }

}

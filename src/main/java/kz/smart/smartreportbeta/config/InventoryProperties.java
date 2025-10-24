package kz.smart.smartreportbeta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "inventory")
public class InventoryProperties {

    /**
     * Сколько секунд считать устройство «онлайн» после последнего события.
     * По умолчанию 10 минут.
     */
    private int onlineThresholdSeconds = 180;

    public int getOnlineThresholdSeconds() {
        return onlineThresholdSeconds;
    }

    public void setOnlineThresholdSeconds(int onlineThresholdSeconds) {
        this.onlineThresholdSeconds = onlineThresholdSeconds;
    }
}

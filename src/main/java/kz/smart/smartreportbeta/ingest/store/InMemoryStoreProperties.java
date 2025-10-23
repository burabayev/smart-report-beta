package kz.smart.smartreportbeta.ingest.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ingest.in-memory")
public class InMemoryStoreProperties {
    private int perDeviceLimit = 1000;
    private int rawFramesLimit = 500;
    private int globalDeviceLimit = 100_000;

    public int getPerDeviceLimit() {
        return perDeviceLimit;
    }

    public void setPerDeviceLimit(int v) {
        this.perDeviceLimit = v;
    }
    public int getRawFramesLimit() {
        return rawFramesLimit;
    }

    public void setRawFramesLimit(int v) {
        this.rawFramesLimit = v;
    }

    public int getGlobalDeviceLimit() {
        return globalDeviceLimit;
    }

    public void setGlobalDeviceLimit(int v) {
        this.globalDeviceLimit = v;
    }
}

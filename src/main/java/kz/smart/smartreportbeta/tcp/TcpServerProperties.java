package kz.smart.smartreportbeta.tcp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tcp")
public class TcpServerProperties {
    private String host = "0.0.0.0";
    private int port = 9000;
    private boolean sendUtcOnConnect = true;
    private boolean logHex = true;
    private boolean wireLog = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSendUtcOnConnect() {
        return sendUtcOnConnect;
    }

    public void setSendUtcOnConnect(boolean v) {
        this.sendUtcOnConnect = v;
    }

    public boolean isLogHex() {
        return logHex;
    }

    public void setLogHex(boolean logHex) {
        this.logHex = logHex;
    }

    public boolean isWireLog() {
        return wireLog;
    }

    public void setWireLog(boolean wireLog) {
        this.wireLog = wireLog;
    }
}

package kz.smart.smartreportbeta.protocol.model;

public record GatewayStatus(
        AlarmType alarmType,
        boolean powerConnected,          // Bit7
        boolean lastPacketOfIndex,       // Bit6
        double batteryVoltageV,          // ед. 10 mV
        double inputVoltageV             // ед. 10 mV
) {
}

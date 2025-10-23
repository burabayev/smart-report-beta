package kz.smart.smartreportbeta.protocol.model;

public enum AlarmType {
    INTERVAL(0xAA),
    LOW_BATTERY(0x10),
    BEGIN_CHARGE(0x60),
    END_CHARGE(0x61),
    UNKNOWN(-1);

    public final int code;

    AlarmType(int c) {
        this.code=c;
    }

    public static AlarmType of(int b) {
        return switch (b & 0xFF) {
            case 0xAA -> INTERVAL;
            case 0x10 -> LOW_BATTERY;
            case 0x60 -> BEGIN_CHARGE;
            case 0x61 -> END_CHARGE;
            default -> UNKNOWN;
        };
    }
}

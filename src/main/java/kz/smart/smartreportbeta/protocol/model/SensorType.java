package kz.smart.smartreportbeta.protocol.model;

public enum SensorType {
    TAG_07_08_08B_08L_09(0x01),
    TAG_08B_DECIPERCENT(0x04);

    public final int code;

    SensorType(int c) {
        this.code=c;
    }

    public static SensorType of(int b) {
        return switch (b & 0xFF) {
            case 0x01 -> TAG_07_08_08B_08L_09;
            case 0x04 -> TAG_08B_DECIPERCENT;
            default -> TAG_07_08_08B_08L_09; // по умолчанию самый общий
        };
    }
}

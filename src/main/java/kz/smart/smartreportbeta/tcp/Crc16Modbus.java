package kz.smart.smartreportbeta.tcp;

public class Crc16Modbus {
    private Crc16Modbus() {}

    public static int crc(byte[] data, int off, int len) {
        int crc = 0xFFFF;
        for (int i = 0; i < len; i++) {
            crc ^= (data[off + i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) crc = (crc >>> 1) ^ 0xA001; else crc = (crc >>> 1);
            }
        }
        // вернуть как MSB first (big-endian) для сравнения с пакетом
        int lo = (crc & 0xFF);
        int hi = ((crc >>> 8) & 0xFF);
        return (hi << 8) | lo;
    }
}

package kz.smart.smartreportbeta.protocol;

import kz.smart.smartreportbeta.protocol.model.*;
import kz.smart.smartreportbeta.protocol.util.Bcd;
import kz.smart.smartreportbeta.protocol.util.Bytes;
import kz.smart.smartreportbeta.protocol.util.Rtc;
import kz.smart.smartreportbeta.tcp.Crc16Modbus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Rd07PacketParser {

    /** Парсит «тело» кадра (то, что у нас приходит без 'TZ' и длины, но с 0D0A в конце). */
    public Rd07Packet parse(byte[] frame) {
        // убрать стоповые 0D 0A
        int n = frame.length;
        if (n < 8 || frame[n - 2] != 0x0D || frame[n - 1] != 0x0A) throw new IllegalArgumentException("Bad frame tail");
        int idxPos = n - 6;
        int packetIndex = Bytes.u16be(frame, idxPos);
        int crcGiven    = Bytes.u16be(frame, idxPos + 2);
        int crcCalc     = Crc16Modbus.crc(frame, 0,idxPos + 2); // CRC по протоколу: от '$$' до PacketIndex включительно
        boolean crcOk   = (crcCalc == crcGiven);

        int p = 0;
        // 1) Protocol '$$'
        if (Bytes.u16be(frame, p) != 0x2424) throw new IllegalArgumentException("Not a '$$' packet");
        p += 2;

        // 2) Hardware type (пропустим)
        p += 2;

        // 3) Firmware (4 байта → "a.b" если первые 2 байта как a,b)
        int fwMajor = Bytes.u8(frame, p);
        int fwMinor = Bytes.u8(frame,p + 1);
        String firmware = fwMajor + "." + fwMinor;
        p += 4;

        // 4) IMEI (8 байт BCD)
        String imei = Bcd.toDigits(frame, p,8);
        p += 8;

        // 5) RTC (6 байт, UTC)
        Instant gwRtc = Rtc.fromPacketSix(frame, p);
        p += 6;

        // 6) Reserved (2)
        p += 2;

        // 7) Status length
        int statusLen = Bytes.u16be(frame, p);
        p += 2;
        GatewayStatus gwStatus = null;
        if (statusLen >= 8) {
            int alarm   = Bytes.u8(frame, p); p += 1;
            int termInf = Bytes.u8(frame, p); p += 1;

            p += 2; // reserved

            double battV = Bytes.u16be(frame, p) / 100.0; p += 2;   // ед. 10 mV
            double inV   = Bytes.u16be(frame, p) / 100.0; p += 2;
            gwStatus = new GatewayStatus(
                    AlarmType.of(alarm),
                    (termInf & 0x80) != 0,
                    (termInf & 0x40) != 0,
                    battV, inV
            );
            // extension B = 0 (нет байтов)
        } else if (statusLen == 0) {
            // нет статуса — ок
        } else {
            p += statusLen; // на всякий случай скип
        }

        // 8) Sensor information data length
        int sensorLen = Bytes.u16be(frame, p); p += 2;
        List<SensorReading> sensors = new ArrayList<>();
        if (sensorLen > 0) {
            SensorType type = SensorType.of(Bytes.u8(frame, p)); p += 1;
            int count = Bytes.u8(frame, p); p += 1;
            int per   = Bytes.u8(frame, p); p += 1;
            for (int i = 0; i < count; i++){
                int s0 = p; // начало записи
                String idHex = String.format("%02X%02X%02X%02X", frame[p], frame[p + 1], frame[p + 2], frame[p + 3]); p += 4;
                int st = Bytes.u8(frame, p); p += 1;
                boolean batteryLow       = (st & 0x80) != 0;
                boolean temperatureAlert = (st & 0x40) != 0;
                boolean buttonPressed    = (st & 0x20) != 0;
                boolean ackRequired      = (st & 0x10) != 0;
                boolean rtcMark          = (st & 0x08) != 0;

                double sBattV = Bytes.u16be(frame, p) / 1000.0; p += 2; // ед. 1 mV
                int tRaw = Bytes.u16be(frame, p); p += 2;
                Double tC;
                if ((tRaw & 0x8000) != 0) tC = null; // abnormal
                else {
                    int sign = ((tRaw & 0x4000) != 0) ? -1 : 1;
                    int val = tRaw & 0x3FFF;
                    tC = sign * (val / 10.0);
                }
                Double h = null;
                if (type == SensorType.TAG_08B_DECIPERCENT) {
                    int hRaw = Bytes.u16be(frame, p); p += 2;
                    if (hRaw != 0xFFFF) h = hRaw / 10.0;
                } else {
                    int h1 = Bytes.u8(frame, p); p += 1;
                    if (h1 != 0xFF) h = (double)h1;
                }
                int rssi = -Bytes.u8(frame, p); p += 1; // «unit: -dBm», т.е. 0x30 -> -48 dBm
                Instant sRtc = Rtc.fromPacketSix(frame, p); p += 6;

                sensors.add(new SensorReading(
                        type,
                        idHex,
                        batteryLow,
                        temperatureAlert,
                        buttonPressed,
                        ackRequired,
                        rtcMark,
                        tC,
                        h,
                        sBattV,
                        rssi,
                        sRtc
                ));
                // safety skip to перезаписанных форматов
                p = s0 + per;
            }
            // extension C,D = 0
        }

        return new Rd07Packet(
                imei,
                firmware,
                gwRtc,
                gwStatus,
                sensors,
                packetIndex,
                crcGiven,
                crcCalc,
                crcOk);
    }
}

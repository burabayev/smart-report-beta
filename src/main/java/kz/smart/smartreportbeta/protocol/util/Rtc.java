package kz.smart.smartreportbeta.protocol.util;

import java.time.*;
public class Rtc {
    private Rtc() {}

    /** year=2000+yy, bytes: yy mm dd HH MM SS (все BCD/hex-байты как в протоколе) */
    public static Instant fromPacketSix(byte[] a,int i) {
        int yy = Bytes.u8(a,i), mm=Bytes.u8(a,i+1), dd=Bytes.u8(a,i+2),
                HH=Bytes.u8(a,i+3), MM=Bytes.u8(a,i+4), SS=Bytes.u8(a,i+5);
        return ZonedDateTime.of(2000+yy, mm, dd, HH, MM, SS, 0, ZoneOffset.UTC).toInstant();
    }
}

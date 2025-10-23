package kz.smart.smartreportbeta.protocol.util;

public class Bytes {
    private Bytes() {}
    public static int u8(byte[] a, int i) {
        return a[i]&0xFF;
    }
    public static int u16be(byte[] a, int i) {
        return ((a[i]&0xFF)<<8)|(a[i+1]&0xFF);
    }
    public static long u32be(byte[] a, int i) {
        return ((long)u16be(a,i)<<16)|u16be(a,i + 2);
    }
}

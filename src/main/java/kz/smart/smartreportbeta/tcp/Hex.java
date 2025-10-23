package kz.smart.smartreportbeta.tcp;

public class Hex {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    public static String of(byte[] a, int off, int len) {
        StringBuilder sb = new StringBuilder(len*3);
        for (int i = 0; i < len; i++) {
            int b = a[off+i] & 0xFF;
            sb.append(HEX[b>>>4]).append(HEX[b&0xF]);
            if (i < len-1) sb.append(' ');
        }
        return sb.toString();
    }
}

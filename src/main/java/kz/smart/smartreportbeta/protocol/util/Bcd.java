package kz.smart.smartreportbeta.protocol.util;

public class Bcd {
    private Bcd() {}

    public static String toDigits(byte[] a, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for(int i = 0; i < len; i++){
            int v=a[off + i]&0xFF;
            sb.append((char)('0'+((v>>>4)&0xF))).append((char)('0'+(v&0xF)));
        }
        // убрать возможный ведущий '0'
        int i = 0;
        while(i < sb.length()-1 && sb.charAt(i) == '0') i++;
        return sb.substring(i);
    }
}

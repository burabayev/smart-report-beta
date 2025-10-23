package kz.smart.smartreportbeta.ingest.event;

import java.net.SocketAddress;
import java.time.Instant;

public record AckSentEvent(
        String imei,
        int packetIndex,
        boolean crcOk,
        Instant at,
        SocketAddress remote
) {
}

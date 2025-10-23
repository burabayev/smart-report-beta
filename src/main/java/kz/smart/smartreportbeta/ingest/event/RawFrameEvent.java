package kz.smart.smartreportbeta.ingest.event;

import java.net.SocketAddress;
import java.time.Instant;

public record RawFrameEvent(
        byte[] frameBodyPlusStop, // то, что логируем как RX[...] (без TZ+Len)
        Instant at,
        SocketAddress remote
) {
}

package kz.smart.smartreportbeta.ingest.event;

import kz.smart.smartreportbeta.protocol.model.Rd07Packet;

import java.net.SocketAddress;
import java.time.Instant;

public record PacketParsedEvent(
        Rd07Packet packet,
        Instant ingestTs,
        SocketAddress remote
) {
}

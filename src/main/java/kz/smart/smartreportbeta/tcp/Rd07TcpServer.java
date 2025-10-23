package kz.smart.smartreportbeta.tcp;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import kz.smart.smartreportbeta.ingest.event.AckSentEvent;
import kz.smart.smartreportbeta.ingest.event.PacketParsedEvent;
import kz.smart.smartreportbeta.ingest.event.RawFrameEvent;
import kz.smart.smartreportbeta.protocol.Rd07PacketParser;
import kz.smart.smartreportbeta.protocol.model.Rd07Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class Rd07TcpServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(Rd07TcpServer.class);

    private final TcpServerProperties props;
    private final ApplicationEventPublisher events;

    private DisposableServer server;
    private volatile boolean running;

    private final Rd07PacketParser parser = new Rd07PacketParser();

    // --- METRICS ---
    private final AtomicInteger mConnections;
    private final Counter mPacketsReceived;
    private final Counter mPacketsParsed;
    private final Counter mPacketsCrcFailed;
    private final Counter mRxBytes;
    private final Counter mAcksSent;

    public Rd07TcpServer(TcpServerProperties props,
                         ApplicationEventPublisher events,
                         MeterRegistry registry) {
        this.props = props;
        this.events = events;

        // Gauge: активные подключения
        this.mConnections = new AtomicInteger(0);
        Gauge.builder("rd07_connections", mConnections, AtomicInteger::get)
                .description("Active TCP connections from RD07 gateways")
                .register(registry);

        // Counters
        this.mPacketsReceived = Counter.builder("rd07_packets_received")
                .description("Total RD07 packets received")
                .register(registry);

        this.mPacketsParsed = Counter.builder("rd07_packets_parsed")
                .description("Total RD07 packets successfully parsed")
                .register(registry);

        this.mPacketsCrcFailed = Counter.builder("rd07_packets_crc_failed")
                .description("Total RD07 packets with CRC mismatch")
                .register(registry);

        this.mRxBytes = Counter.builder("rd07_rx_bytes")
                .description("Total bytes received from RD07")
                .register(registry);

        this.mAcksSent = Counter.builder("rd07_acks_sent")
                .description("Total ACKs sent to RD07")
                .register(registry);
    }

    @Override
    public void start() {
        if (running) return;

        server = TcpServer.create()
                .host(props.getHost())
                .port(props.getPort())
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .doOnBound(s -> log.info("Listening on {}:{}", props.getHost(), s.port()))
                .doOnConnection(conn -> {
                    SocketAddress remote = conn.address();
                    log.info("RD07 connected: {}", remote);
                    mConnections.incrementAndGet();

                    // декремент при закрытии сокета
                    conn.onDispose(() -> {
                        mConnections.decrementAndGet();
                        log.info("RD07 disconnected: {}", remote);
                    });

                    if (props.isWireLog()) {
                        conn.addHandlerLast(new LoggingHandler("NETTY-WIRE", LogLevel.DEBUG));
                    }

                    if (props.isSendUtcOnConnect()) {
                        String utcCmd = "@UTC," + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneOffset.UTC).format(Instant.now()) + "#";
                        conn.outbound()
                                .sendString(Mono.just(utcCmd), StandardCharsets.US_ASCII)
                                .then()
                                .subscribe(
                                        null,
                                        err -> log.warn("UTC send failed: {}", err.toString()),
                                        () -> log.info("UTC sent: {}", utcCmd)
                                );
                    }

                    // Декодер кадров RD07 (вырезает 'TZ'+Len и отдаёт Body+Stop)
                    conn.addHandlerLast(new Rd07FrameDecoder());
                })
                .handle((inbound, outbound) -> {
                    AtomicReference<SocketAddress> remoteRef = new AtomicReference<>();
                    inbound.withConnection(c -> remoteRef.set(c.address()));

                    Flux<String> acks = inbound
                            .receiveObject()
                            .cast(ByteBuf.class)
                            .map(buf -> {
                                byte[] frame = new byte[buf.readableBytes()];
                                buf.readBytes(frame);

                                // Метрики: приём кадра
                                mPacketsReceived.increment();
                                mRxBytes.increment(frame.length);

                                // публикуем "сырое" событие (для /debug/raw)
                                events.publishEvent(new RawFrameEvent(frame, Instant.now(), remoteRef.get()));

                                if (props.isLogHex()) {
                                    log.info("RX[{}]: {}", frame.length, Hex.of(frame, 0, frame.length));
                                }

                                if (frame.length < 6) {
                                    return null; // защита от мусора
                                }

                                // последние 6 байт: [idx(2)][crc(2)][0D 0A]
                                int idxPos = frame.length - 6;
                                int packetIndex = ((frame[idxPos] & 0xFF) << 8) | (frame[idxPos + 1] & 0xFF);
                                int crcGiven = ((frame[idxPos + 2] & 0xFF) << 8) | (frame[idxPos + 3] & 0xFF);
                                int crcCalc = Crc16Modbus.crc(frame, 0, idxPos + 2); // включаем PacketIndex в CRC

                                String imeiForAckEvent = null;

                                // парсим кадр в доменную модель (и логируем красиво)
                                try {
                                    Rd07Packet p = parser.parse(frame);
                                    imeiForAckEvent = p.imei();

                                    log.info("PARSED imei={} fw={} rtcUtc={} sensors={} idx={} crcOk={} (gwBatt={}V in={}V alarm={})",
                                            p.imei(), p.firmware(), p.gatewayRtcUtc(), p.sensors().size(),
                                            p.packetIndex(), p.crcOk(),
                                            p.status() != null ? String.format("%.2f", p.status().batteryVoltageV()) : "-",
                                            p.status() != null ? String.format("%.2f", p.status().inputVoltageV()) : "-",
                                            p.status() != null ? p.status().alarmType() : "-"
                                    );

                                    if (!p.sensors().isEmpty()) {
                                        var s = p.sensors().get(0);
                                        log.info("SENSOR[0] id={} t={}C h={}{} batt={}V rssi={}dBm ts={} (flags:lowBatt={} tempAlert={} btn={} ack={} rtcMark={})",
                                                s.idHex(),
                                                s.temperatureC() == null ? "abn" : String.format("%.1f", s.temperatureC()),
                                                s.humidityPercent() == null ? "-" : String.format("%.1f", s.humidityPercent()),
                                                s.type().name().contains("DECIPERCENT") ? "(0.1%)" : "(%)",
                                                String.format("%.3f", s.batteryV()),
                                                s.rssiDbm(), s.sensorRtcUtc(),
                                                s.batteryLow(), s.temperatureAlert(), s.buttonPressed(), s.ackRequired(), s.rtcMark()
                                        );
                                    }

                                    // метрика: успешный парсинг
                                    mPacketsParsed.increment();

                                    // публикуем "распарсено"
                                    events.publishEvent(new PacketParsedEvent(p, Instant.now(), remoteRef.get()));
                                } catch (Exception ex) {
                                    log.warn("PARSE ERROR: {}", ex.getMessage());
                                }

                                if (crcCalc != crcGiven) {
                                    // метрика: CRC ошибка
                                    mPacketsCrcFailed.increment();
                                    log.warn("CRC mismatch: calc=0x{} given=0x{} (ack anyway)",
                                            Integer.toHexString(crcCalc), Integer.toHexString(crcGiven));
                                }

                                String ack = "@ACK," + String.format("%04d", packetIndex) + "#";
                                log.info("ACK -> {}", ack);

                                // публикуем факт отправки ACK (событие создаем здесь)
                                events.publishEvent(new AckSentEvent(
                                        imeiForAckEvent, packetIndex, crcCalc == crcGiven, Instant.now(), remoteRef.get()
                                ));

                                return ack;
                            })
                            .filter(s -> s != null)
                            // метрика: считаем реально отправляемые ACK'и
                            .doOnNext(s -> mAcksSent.increment());

                    // Отправляем ACK'и и держим соединение открытым
                    return outbound.sendString(acks, StandardCharsets.US_ASCII).neverComplete();
                })
                .bindNow();

        running = true;
        log.info("RD07 TCP server started on port {}", server.port());
    }

    @Override
    public void stop() {
        if (!running) return;
        server.disposeNow();
        running = false;
        log.info("RD07 TCP server stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}

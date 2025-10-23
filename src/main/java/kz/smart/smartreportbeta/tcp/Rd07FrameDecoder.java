package kz.smart.smartreportbeta.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * RD07: Frame = 'TZ'(2) + Len(2) + Body(L) + Stop(2=0D0A)
 * Len: от ProtocolType до CheckCode (включительно). Stop вне длины.
 */
public class Rd07FrameDecoder extends ByteToMessageDecoder {
    private static final short START_T = 0x54; // 'T'
    private static final short START_Z = 0x5A; // 'Z'

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();
        while (in.readableBytes() >= 2) {
            in.markReaderIndex();
            short b1 = in.readUnsignedByte();
            short b2 = in.readUnsignedByte();
            if (b1 == START_T && b2 == START_Z) {
                // start ok. Need len (2) + rest
                if (in.readableBytes() < 2) {
                    in.resetReaderIndex();
                    return;
                }
                int len = in.readUnsignedShort(); // MSB first
                int totalNeeded = len /*body*/ + 2 /*stop*/;
                if (in.readableBytes() < totalNeeded) {
                    in.resetReaderIndex();
                    return;
                }
                // body + stop
                ByteBuf frame = in.readSlice(len + 2).retain();
                out.add(frame);
                return;
            } else {
                // shift by one and continue scanning
                in.resetReaderIndex();
                in.readByte(); // drop one
            }
        }
    }
}

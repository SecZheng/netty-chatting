package com.sec.chatting.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MagicCodec extends ByteToMessageCodec<ByteBuf> {
    static final String MAGIC = "$sec";
    static final Charset CHARSET = StandardCharsets.UTF_8;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        out.writeBytes(MAGIC.getBytes(CHARSET));
        out.writeBytes(msg);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        CharSequence str = in.readCharSequence(4, CHARSET);
        if (MAGIC.equals(str)) {
            out.add(ctx.alloc().buffer().writeBytes(in));
        }
    }
}

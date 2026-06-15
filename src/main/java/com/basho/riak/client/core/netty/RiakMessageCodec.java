/*
 * Copyright (c) 2013 Basho Technologies Inc.
 * Copyright (c) 2026 Workday, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.basho.riak.client.core.netty;

import com.basho.riak.client.core.RiakMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;

/**
 *
 * @author Brian Roach <roach at basho dot com>
 * modified by George Madi <george.madi@workday.com>
 *      - added incremental frame consumption to allow very large objects to be read quickly
 */
public class RiakMessageCodec extends ByteToMessageCodec<RiakMessage>
{
    /** Riak PB frame header: 4-byte length prefix plus 1-byte message code. */
    private static final int FRAME_HEADER_BYTES = 5;

    // In-progress frame state. Consuming each frame incrementally as bytes
    // arrive keeps Netty's accumulation buffer tiny; waiting for a full frame
    // would force the accumulation buffer to repeatedly reallocate and copy, which is
    // O(n^2) for large (multi-GB) responses.
    private byte code;
    private byte[] payload;
    private int payloadOffset;

    @Override
    protected void encode(ChannelHandlerContext ctx, RiakMessage msg, ByteBuf out) throws Exception
    {
        int length = msg.getData().length + 1;
        out.writeInt(length);
        out.writeByte(msg.getCode());
        out.writeBytes(msg.getData());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        while (true)
        {
            if (payload == null)
            {
                // Wait for the length prefix and message code; at most 4
                // bytes are ever retained by the accumulation buffer between reads.
                if (in.readableBytes() < FRAME_HEADER_BYTES)
                {
                    return;
                }
                int length = in.readInt();
                code = in.readByte();
                payload = new byte[length - 1];
                payloadOffset = 0;
            }

            int toCopy = Math.min(in.readableBytes(), payload.length - payloadOffset);
            if (toCopy > 0)
            {
                in.readBytes(payload, payloadOffset, toCopy);
                payloadOffset += toCopy;
            }

            if (payloadOffset < payload.length)
            {
                return;
            }

            out.add(new RiakMessage(code, payload));
            payload = null;
        }
    }
}

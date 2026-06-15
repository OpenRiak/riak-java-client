/*
 * Copyright 2013 (c) Basho Technologies Inc.
 * Copyright 2026 (c) Workday, Inc.
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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.powermock.reflect.Whitebox;

/**
 *
 * @author Brian Roach <roach at basho dot com>
 * @since 2.0
 * modified by George Madi <george.madi@workday.com>
 *      - added extra tests for incremental frame consumption to allow
 *          very large objects to be read quickly
 */
public class RiakMessageCodecTest
{
    final private int SIZE_DATA = 30;
    private byte[] data;
    final private int SIZE_LENGTH = 4;
    final private int SIZE_CODE = 1;
    private byte code = 1;
    private ByteBuf buffer;
    private ChannelHandlerContext mockContext;

    @Before
    public void setUp() throws Exception
    {
        code = 1;
        data = new byte[SIZE_DATA];
        Arrays.fill(data, (byte)1);

        RiakMessage pbMessage = new RiakMessage(code, data);
        RiakMessageCodec codec = new RiakMessageCodec();

        mockContext = mock(ChannelHandlerContext.class);
        buffer = Unpooled.buffer();
        Whitebox.invokeMethod(codec, "encode", mockContext, pbMessage, buffer);
    }

    @Test
    public void encode() throws Exception
    {
        assertTrue(buffer.isReadable());
        assertEquals(buffer.readableBytes(), SIZE_DATA + SIZE_LENGTH + SIZE_CODE);

        int encodedLength = buffer.readInt();
        byte encodedCode = buffer.readByte();
        byte[] encodedData = new byte[SIZE_DATA];

        buffer.readBytes(encodedData);

        assertEquals(encodedCode, code);
        assertEquals(encodedLength, SIZE_DATA + SIZE_CODE);
        assertArrayEquals(data, encodedData);
    }

    @Test
    public void decode() throws Exception
    {
        RiakMessageCodec codec = new RiakMessageCodec();
        List<Object> outList = new ArrayList<>();
        Whitebox.invokeMethod(codec, "decode", mockContext, buffer, outList);
        RiakMessage message = (RiakMessage) outList.get(0);
        assertEquals(code, message.getCode());
        assertArrayEquals(data, message.getData());
    }

    @Test
    public void decodeFragmentedHeader() throws Exception
    {
        // Only 4 bytes arrive first (length prefix, no code byte yet)
        RiakMessageCodec codec = new RiakMessageCodec();
        List<Object> outList = new ArrayList<>();
        ByteBuf buf = Unpooled.buffer();

        buf.writeInt(SIZE_DATA + SIZE_CODE);
        Whitebox.invokeMethod(codec, "decode", mockContext, buf, outList);
        assertTrue("No message yet with only length prefix", outList.isEmpty());

        buf.writeByte(code);
        buf.writeBytes(data);
        Whitebox.invokeMethod(codec, "decode", mockContext, buf, outList);

        assertEquals(1, outList.size());
        RiakMessage message = (RiakMessage) outList.get(0);
        assertEquals(code, message.getCode());
        assertArrayEquals(data, message.getData());
    }

    @Test
    public void decodeFragmentedPayload() throws Exception
    {
        // Full header arrives, then payload in two chunks
        RiakMessageCodec codec = new RiakMessageCodec();
        List<Object> outList = new ArrayList<>();
        ByteBuf buf = Unpooled.buffer();

        buf.writeInt(SIZE_DATA + SIZE_CODE);
        buf.writeByte(code);
        buf.writeBytes(data, 0, SIZE_DATA / 2);
        Whitebox.invokeMethod(codec, "decode", mockContext, buf, outList);
        assertTrue("No message yet with partial payload", outList.isEmpty());

        buf.writeBytes(data, SIZE_DATA / 2, SIZE_DATA - (SIZE_DATA / 2));
        Whitebox.invokeMethod(codec, "decode", mockContext, buf, outList);

        assertEquals(1, outList.size());
        RiakMessage message = (RiakMessage) outList.get(0);
        assertEquals(code, message.getCode());
        assertArrayEquals(data, message.getData());
    }

    @Test
    public void decodeOneByteAtATime() throws Exception
    {
        RiakMessageCodec codec = new RiakMessageCodec();
        List<Object> outList = new ArrayList<>();
        ByteBuf buf = Unpooled.buffer();

        byte[] rawFrame = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), rawFrame);

        for (int i = 0; i < rawFrame.length - 1; i++)
        {
            buf.writeByte(rawFrame[i]);
            Whitebox.invokeMethod(codec, "decode", mockContext, buf, outList);
            assertTrue("No message at byte " + i, outList.isEmpty());
        }

        buf.writeByte(rawFrame[rawFrame.length - 1]);
        Whitebox.invokeMethod(codec, "decode", mockContext, buf, outList);

        assertEquals(1, outList.size());
        RiakMessage message = (RiakMessage) outList.get(0);
        assertEquals(code, message.getCode());
        assertArrayEquals(data, message.getData());
    }

    @Test
    public void decodeTwoConsecutiveMessages() throws Exception
    {
        // Two complete messages in one buffer — exercises while loop
        RiakMessageCodec codec = new RiakMessageCodec();
        List<Object> outList = new ArrayList<>();
        ByteBuf buf = Unpooled.buffer();

        buf.writeInt(SIZE_DATA + SIZE_CODE);
        buf.writeByte(code);
        buf.writeBytes(data);
        buf.writeInt(SIZE_DATA + SIZE_CODE);
        buf.writeByte(code);
        buf.writeBytes(data);

        Whitebox.invokeMethod(codec, "decode", mockContext, buf, outList);

        assertEquals(2, outList.size());
        for (Object obj : outList)
        {
            RiakMessage message = (RiakMessage) obj;
            assertEquals(code, message.getCode());
            assertArrayEquals(data, message.getData());
        }
    }
}

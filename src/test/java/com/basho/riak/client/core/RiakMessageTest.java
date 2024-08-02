package com.basho.riak.client.core;

import com.basho.riak.client.core.netty.RiakResponseException;
import com.basho.riak.protobuf.RiakKvPB;
import com.basho.riak.protobuf.RiakMessageCodes;
import com.basho.riak.protobuf.RiakPB;
import com.ericsson.otp.erlang.*;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Luke Bakken <lbakken at basho dot com>
 */
public class RiakMessageTest {
    @Test
    public void parsesPbufErrorCorrectly() {
        RiakPB.RpbErrorResp.Builder b = RiakPB.RpbErrorResp.newBuilder();
        b.setErrcode(1234);
        b.setErrmsg(ByteString.copyFromUtf8("this is an error"));
        RiakPB.RpbErrorResp rpbErrorResp = b.build();

        RiakMessage msg = new RiakMessage(RiakMessageCodes.MSG_ErrorResp, rpbErrorResp.toByteArray());
        RiakResponseException err = msg.getRiakError();
        Assert.assertEquals("this is an error", err.getMessage());
        Assert.assertEquals(1234, err.getCode());
    }

    @Test
    public void parsesTtbErrorCorrectly() {
        final byte[] TTB_ERROR = {(byte) 131, 104, 3, 100, 0, 12, 114, 112, 98, 101, 114,
                114, 111, 114, 114, 101, 115, 112, 109, 0, 0, 0, 16, 116, 104, 105, 115, 32, 105, 115,
                32, 97, 110, 32, 101, 114, 114, 111, 114, 98, 0, 0, 4, (byte) 210};

        RiakMessage msg = new RiakMessage(RiakMessageCodes.MSG_TsTtbMsg, TTB_ERROR);
        RiakResponseException err = msg.getRiakError();
        Assert.assertEquals("this is an error", err.getMessage());
        Assert.assertEquals(1234, err.getCode());
    }

    @Test
    public void parseCopyRespErrorCorrectly() {
        RiakKvPB.RpbCloneResp.Builder b = RiakKvPB.RpbCloneResp.newBuilder();

        OtpOutputStream os = new OtpOutputStream();
        new OtpErlangAtom("error").encode(os);
        b.setError(ByteString.copyFrom(os.toByteArray()));

        RiakMessage msg = new RiakMessage(RiakMessageCodes.MSG_CloneResp, b.build().toByteArray());

        assertTrue(msg.isRiakError());
        assertEquals(msg.getRiakError().getMessage(), "error");
        assertEquals(msg.getRiakError().getCode(), 0);
    }

    @Test
    public void parseCopyRespErrorCorrectly_tupleAtom() {
        RiakKvPB.RpbCloneResp.Builder b = RiakKvPB.RpbCloneResp.newBuilder();

        OtpOutputStream os = new OtpOutputStream();
        OtpErlangObject[] objects = {
                new OtpErlangAtom("error")
        };
        new OtpErlangTuple(objects).encode(os);

        b.setError(ByteString.copyFrom(os.toByteArray()));

        RiakMessage msg = new RiakMessage(RiakMessageCodes.MSG_CloneResp, b.build().toByteArray());

        assertTrue(msg.isRiakError());
        assertEquals(msg.getRiakError().getMessage(), "error");
        assertEquals(msg.getRiakError().getCode(), 0);
    }

    @Test
    public void parseCopyRespErrorCorrectly_tupleAtomInt() {
        RiakKvPB.RpbCloneResp.Builder b = RiakKvPB.RpbCloneResp.newBuilder();

        OtpOutputStream os = new OtpOutputStream();
        OtpErlangObject[] objects = {
                new OtpErlangAtom("error"),
                new OtpErlangInt(123)
        };
        new OtpErlangTuple(objects).encode(os);

        b.setError(ByteString.copyFrom(os.toByteArray()));

        RiakMessage msg = new RiakMessage(RiakMessageCodes.MSG_CloneResp, b.build().toByteArray());

        assertTrue(msg.isRiakError());
        assertEquals(msg.getRiakError().getMessage(), "error");
        assertEquals(msg.getRiakError().getCode(), 123);
    }

    @Test
    public void parseCopyRespErrorCorrectly_tupleAtomIntInt() {
        RiakKvPB.RpbCloneResp.Builder b = RiakKvPB.RpbCloneResp.newBuilder();

        OtpOutputStream os = new OtpOutputStream();
        OtpErlangObject[] objects = {
                new OtpErlangAtom("error"),
                new OtpErlangInt(123)
        };
        new OtpErlangTuple(objects).encode(os);

        b.setError(ByteString.copyFrom(os.toByteArray()));

        RiakMessage msg = new RiakMessage(RiakMessageCodes.MSG_CloneResp, b.build().toByteArray());

        assertTrue(msg.isRiakError());
        assertEquals(msg.getRiakError().getMessage(), "error");
        assertEquals(msg.getRiakError().getCode(), 123);
    }

}

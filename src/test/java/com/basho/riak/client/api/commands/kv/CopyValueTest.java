package com.basho.riak.client.api.commands.kv;

import com.basho.riak.client.api.cap.BasicVClock;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.commands.MockedResponseOperationTest;
import com.basho.riak.client.core.operations.CloneOperation;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.protobuf.RiakKvPB;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class CopyValueTest extends MockedResponseOperationTest<CloneOperation, CloneOperation.Response> {

    public CopyValueTest() {
        super(CloneOperation.Response.class);
    }

    private final Location src = new Location(new Namespace("type", "bucket"), "src");
    private final Location dst = new Location(new Namespace("type", "bucket"), "dst");
    private final VClock vClock = new BasicVClock(new byte[]{'1'});


    @Override
    protected void setupResponse(CloneOperation.Response mockedResponse) {
        super.setupResponse(mockedResponse);

        when(mockedResponse.getObjectList()).thenReturn(new ArrayList<>());
    }

    @Test
    public void testCopy() throws Exception {
        CopyValue.Builder copy = new CopyValue.Builder(src, dst)
                .withOption(CopyValue.Option.SrcVClock, vClock)
                .withOption(CopyValue.Option.DST_PROVENANCE_METADATA, CloneOperation.Builder.ProvenanceMetadata.STORE)
                .withOption(CopyValue.Option.RETURN_BODY, true)
                .withOption(CopyValue.Option.R, new Quorum(1))
                .withOption(CopyValue.Option.PR, new Quorum(2))
                .withOption(CopyValue.Option.W, new Quorum(3))
                .withOption(CopyValue.Option.PW, new Quorum(4))
                .withOption(CopyValue.Option.DW, new Quorum(5))
                .withOption(CopyValue.Option.RW, new Quorum(6))
                .withOption(CopyValue.Option.N_VAL, 7)
                .withOption(CopyValue.Option.TIMEOUT, 8)
                .withOption(CopyValue.Option.RECV_TIMEOUT, 9)
                .withOption(CopyValue.Option.BASIC_QUORUM, true)
                .withOption(CopyValue.Option.SLOPPY_QUORUM, true)
                .withOption(CopyValue.Option.NOTFOUND_OK, true)
                .withOption(CopyValue.Option.ASIS, true)
                .withOption(CopyValue.Option.DETAILS, CloneOperation.Builder.Details.TIMING);

        final CloneOperation operation = executeAndVerify(copy.build());

        RiakKvPB.RpbCloneReq.Builder builder = (RiakKvPB.RpbCloneReq.Builder) Whitebox.getInternalState(operation, "reqBuilder");

        assertEquals(builder.getSrcBucket().toStringUtf8(), "bucket");
        assertEquals(builder.getSrcBucketType().toStringUtf8(), "type");
        assertEquals(builder.getSrcKey().toStringUtf8(), "src");

        assertEquals(builder.getDstBucket().toStringUtf8(), "bucket");
        assertEquals(builder.getDstBucketType().toStringUtf8(), "type");
        assertEquals(builder.getDstKey().toStringUtf8(), "dst");

        // Must be false for Copy!!
        assertFalse(builder.getDeleteSrc());

        assertEquals(builder.getDstProvMeta().toStringUtf8(), "store");
        assertTrue(builder.getReturnBody());
        assertEquals(builder.getR(), 1);
        assertEquals(builder.getPr(), 2);
        assertEquals(builder.getW(), 3);
        assertEquals(builder.getPw(), 4);
        assertEquals(builder.getDw(), 5);
        assertEquals(builder.getRw(), 6);
        assertEquals(builder.getNVal(), 7);
        assertEquals(builder.getTimeout(), 8);
        assertEquals(builder.getRecvTimeout(), 9);
        assertTrue(builder.getBasicQuorum());
        assertTrue(builder.getSloppyQuorum());
        assertTrue(builder.getNotfoundOk());
        assertTrue(builder.getAsis());
        assertEquals(builder.getDetailsList().size(), 1);
        assertEquals(builder.getDetails(0).toStringUtf8(), "TIMING");
    }

}
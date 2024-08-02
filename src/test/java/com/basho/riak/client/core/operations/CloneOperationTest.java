/*
 * Copyright (c) 2024 Workday, Inc.
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
package com.basho.riak.client.core.operations;

import com.basho.riak.client.core.RiakMessage;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.util.BinaryValue;
import com.basho.riak.protobuf.RiakKvPB;
import com.basho.riak.protobuf.RiakMessageCodes;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import static org.junit.Assert.*;

public class CloneOperationTest {

    @Test
    public void testCloneOperationCreateChannelMessage() throws InvalidProtocolBufferException {
        Namespace oldBucket = new Namespace(Namespace.DEFAULT_BUCKET_TYPE, "old_bucket");
        Namespace newBucket = new Namespace(Namespace.DEFAULT_BUCKET_TYPE, "new_bucket");
        BinaryValue srcKey = BinaryValue.create("src_key".getBytes());

        Location oldLocation = new Location(oldBucket, srcKey);
        Location newLocation = new Location(newBucket, srcKey);

        CloneOperation operation = new CloneOperation.Builder(oldLocation, newLocation)
                .withDeleteSrc(true)
                .withDstProvMeta(CloneOperation.Builder.ProvenanceMetadata.STORE)
                .withReturnBody(false)
                .withR(1)
                .withPR(2)
                .withW(3)
                .withPW(4)
                .withDW(5)
                .withRW(6)
                .withNVal(7)
                .withTimeout(7)
                .withRecvTimeout(9)
                .withBasicQuorum(true)
                .withSloppyQuorum(true)
                .withNotFoundOk(true)
                .withAsis(true)
                .withDetails(CloneOperation.Builder.Details.VNODES)
                .build();

        RiakMessage rm = operation.createChannelMessage();

        assertEquals(RiakMessageCodes.MSG_CloneReq, rm.getCode());
        RiakKvPB.RpbCloneReq req = RiakKvPB.RpbCloneReq.parseFrom(rm.getData());

        assertEquals("old_bucket", req.getSrcBucket().toStringUtf8());
        assertEquals("new_bucket", req.getDstBucket().toStringUtf8());

        assertEquals("src_key", req.getSrcKey().toStringUtf8());
        assertEquals("src_key", req.getDstKey().toStringUtf8());

        assertTrue(req.getDeleteSrc());
        assertEquals(req.getDstProvMeta(), ByteString.copyFromUtf8("store"));
        assertFalse(req.getReturnBody());
        assertEquals(req.getR(), 1);
        assertEquals(req.getPr(), 2);
        assertEquals(req.getW(), 3);
        assertEquals(req.getPw(), 4);
        assertEquals(req.getDw(), 5);
        assertEquals(req.getRw(), 6);
        assertEquals(req.getNVal(), 7);
        assertEquals(req.getTimeout(), 7);
        assertEquals(req.getRecvTimeout(), 9);
        assertTrue(req.getBasicQuorum());
        assertTrue(req.getSloppyQuorum());
        assertTrue(req.getNotfoundOk());
        assertTrue(req.getAsis());
        assertFalse(req.hasSyncOnWrite());
        assertEquals(req.getDetails(0), ByteString.copyFromUtf8(CloneOperation.Builder.Details.VNODES.toString()));
    }

    @Test
    public void testCloneOperationCreateChannelMessage_NoOptions() throws InvalidProtocolBufferException {
        Namespace oldBucket = new Namespace(Namespace.DEFAULT_BUCKET_TYPE, "old_bucket");
        Namespace newBucket = new Namespace(Namespace.DEFAULT_BUCKET_TYPE, "new_bucket");
        BinaryValue srcKey = BinaryValue.create("src_key".getBytes());

        Location oldLocation = new Location(oldBucket, srcKey);
        Location newLocation = new Location(newBucket, srcKey);

        CloneOperation operation = new CloneOperation.Builder(oldLocation, newLocation)
                .build();

        RiakMessage rm = operation.createChannelMessage();

        assertEquals(RiakMessageCodes.MSG_CloneReq, rm.getCode());
        RiakKvPB.RpbCloneReq req = RiakKvPB.RpbCloneReq.parseFrom(rm.getData());

        assertEquals("old_bucket", req.getSrcBucket().toStringUtf8());
        assertEquals("new_bucket", req.getDstBucket().toStringUtf8());

        assertEquals("src_key", req.getSrcKey().toStringUtf8());
        assertEquals("src_key", req.getDstKey().toStringUtf8());

        assertFalse(req.getDeleteSrc());
        assertFalse(req.hasDstProvMeta());
        assertFalse(req.hasReturnBody());
        assertFalse(req.hasR());
        assertFalse(req.hasPr());
        assertFalse(req.hasW());
        assertFalse(req.hasPw());
        assertFalse(req.hasDw());
        assertFalse(req.hasRw());
        assertFalse(req.hasNVal());
        assertFalse(req.hasTimeout());
        assertFalse(req.hasRecvTimeout());
        assertFalse(req.hasBasicQuorum());
        assertFalse(req.hasSloppyQuorum());
        assertFalse(req.hasNotfoundOk());
        assertFalse(req.hasAsis());
        assertFalse(req.hasSyncOnWrite());
        assertEquals(req.getDetailsCount(), 0);
    }

}

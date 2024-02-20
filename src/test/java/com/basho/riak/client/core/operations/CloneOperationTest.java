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
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.query.links.RiakLink;
import com.basho.riak.client.core.util.BinaryValue;
import com.basho.riak.protobuf.RiakKvPB;
import com.basho.riak.protobuf.RiakMessageCodes;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CloneOperationTest
{
    @Test
    public void testCloneOperationCreateChannelMessage() throws InvalidProtocolBufferException
    {
        Namespace oldBucket = new Namespace(Namespace.DEFAULT_BUCKET_TYPE, "old_bucket");
        Namespace newBucket = new Namespace(Namespace.DEFAULT_BUCKET_TYPE, "new_bucket");
        BinaryValue srcKey = BinaryValue.create("src_key".getBytes());

        Location oldLocation = new Location(oldBucket, srcKey);
        Location newLocation = new Location(newBucket, srcKey);

        CloneOperation operation =
            new CloneOperation.Builder(oldLocation, newLocation)
                .withGetNVal(2)
                .withDelDw(4)
                .build();

        RiakMessage rm = operation.createChannelMessage();

        assertTrue(rm.getCode() == RiakMessageCodes.MSG_CloneReq);
        RiakKvPB.RpbCloneReq req = RiakKvPB.RpbCloneReq.parseFrom(rm.getData());

        assertTrue(req.getSrcBucket().toStringUtf8().equals("old_bucket"));
        assertTrue(req.getDstBucket().toStringUtf8().equals("new_bucket"));

        assertTrue(req.getSrcKey().toStringUtf8().equals("src_key"));
        assertTrue(req.getDstKey().toStringUtf8().equals("src_key"));

        assertTrue(req.getGetQuorum().getNVal() == 2);
        assertTrue(req.getDelQuorum().getDw() == 4);
    }
}

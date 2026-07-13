/*
 * Copyright 2026 Workday, Inc.
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
package com.basho.riak.client.api.commands;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import org.junit.Assume;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Local smoke test for trace/batch request fields.
 * <p>
 * This test is opt-in and requires a running Riak node on localhost:10017.
 * Enable with: -Dcom.basho.riak.trace.localtest=true
 * </p>
 */
public class TraceBatchLocalNodeTest
{
    @Test
    public void storeFetchDeleteWithTraceAndBatchIds() throws Exception
    {
        Assume.assumeTrue("Enable with -Dcom.basho.riak.trace.localtest=true",
            Boolean.getBoolean("com.basho.riak.trace.localtest"));

        RiakNode node = new RiakNode.Builder()
            .withRemoteAddress("127.0.0.1")
            .withRemotePort(10017)
            .build();
        RiakCluster cluster = new RiakCluster.Builder(Collections.singletonList(node)).build();
        cluster.start();

        try
        {
            RiakClient client = new RiakClient(cluster);

            byte[] traceId = "trace-localhost-001".getBytes(StandardCharsets.UTF_8);
            byte[] batchId = "batch-localhost-001".getBytes(StandardCharsets.UTF_8);

            Namespace ns = new Namespace("default", "trace_test_bucket");
            String key = "trace-test-" + UUID.randomUUID();
            Location loc = new Location(ns, key);

            String payload = "hello-trace";
            StoreValue store = new StoreValue.Builder(payload)
                .withLocation(loc)
                .withOption(StoreValue.Option.TRACE_ID, traceId)
                .withOption(StoreValue.Option.BATCH_ID, batchId)
                .build();
            client.execute(store);

            FetchValue fetch = new FetchValue.Builder(loc)
                .withOption(FetchValue.Option.TRACE_ID, traceId)
                .withOption(FetchValue.Option.BATCH_ID, batchId)
                .build();
            FetchValue.Response fetched = client.execute(fetch);
            assertEquals(payload, fetched.getValue(String.class));

            DeleteValue delete = new DeleteValue.Builder(loc)
                .withOption(DeleteValue.Option.TRACE_ID, traceId)
                .withOption(DeleteValue.Option.BATCH_ID, batchId)
                .build();
            client.execute(delete);

            FetchValue.Response afterDelete = client.execute(new FetchValue.Builder(loc).build());
            assertTrue(afterDelete.isNotFound());
        }
        finally
        {
            cluster.shutdown();
        }
    }
}

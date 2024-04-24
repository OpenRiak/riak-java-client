package com.basho.riak.client.api.commands;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.MultiDelete;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiDeleteTest
{
    @Mock
    RiakCluster mockCluster;
    Location key1 = new Location(new Namespace("type1", "bucket1"), "key1");
    Location key2 = new Location(new Namespace("type2", "bucket2"), "key2");
    RiakClient client;

    @Before
    @SuppressWarnings("unchecked")
    public void init() throws Exception
    {
        client = new RiakClient(mockCluster);
    }

    @Test
    public void testExecuteAsync()
    {
        MultiDelete.Builder multiDeleteBuilder = new MultiDelete.Builder();
        multiDeleteBuilder.withTimeout(3000).addLocations(key1, key2);
        MultiDelete multiDelete = multiDeleteBuilder.build();
        client.executeAsync(multiDelete);
    }
}

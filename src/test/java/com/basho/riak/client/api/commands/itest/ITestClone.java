package com.basho.riak.client.api.commands.itest;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.CloneValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.operations.itest.ITestBase;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import org.junit.Test;
import com.basho.riak.client.core.operations.CloneOperation;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

public class ITestClone extends ITestBase
{
    private final RiakClient client = new RiakClient(cluster);
    private final Namespace booksBucket = new Namespace("books");

    @Test
    public void testCopySucceeds() throws ExecutionException, InterruptedException
    {
        // Insert Data
        Location[] bookLocations = insertBookData(client);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocations[0]).build();
        Book fetchedBook = client.execute(fetchMobyDickOp).getValue(Book.class);
        assertNotNull(fetchedBook);
        

        Location cloneLocation = new Location(booksBucket, "moby_dick_cloned");

        CloneValue cloneBook = new CloneValue.Builder(bookLocations[0], cloneLocation).build();

        CloneValue.Response cloneResp = client.execute(cloneBook);
        System.out.println(cloneResp);

        // Verify data was cloned
        FetchValue fetchClone = new FetchValue.Builder(cloneLocation).build();
        fetchedBook = client.execute(fetchClone).getValue(Book.class);
        assertNotNull(fetchedBook);

        // Verify original still exists
        fetchedBook = client.execute(fetchMobyDickOp).getValue(Book.class);
        assertNotNull(fetchedBook);
    }

    private Location[] insertBookData(RiakClient client)
            throws ExecutionException, InterruptedException
    {
        Location[] bookLocations = new Location[] {
                new Location(booksBucket, "moby_dick"),
        };

        Book mobyDick = new Book();
        mobyDick.title = "Moby Dick";
        mobyDick.author = "Herman Melville";
        mobyDick.body = "Call me Ishmael. Some years ago...";
        mobyDick.isbn = "1111979723";
        mobyDick.copiesOwned = 3;

        StoreValue storeBookOp = new StoreValue.Builder(mobyDick).withLocation(bookLocations[0]).build();
        client.execute(storeBookOp);

        return bookLocations;
    }

    public static class Book
    {
        public String title;
        public String author;
        public String body;
        public String isbn;
        public Integer copiesOwned;
    }
}

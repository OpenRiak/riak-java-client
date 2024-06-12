package com.basho.riak.client.api.commands.itest;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.annotations.RiakVClock;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.commands.kv.CloneValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.operations.itest.ITestBase;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;
import com.basho.riak.client.core.operations.CloneOperation;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.*;

public class ITestClone extends ITestBase
{
    private final RiakClient client = new RiakClient(cluster);
    private final Namespace booksBucket = new Namespace("books");

    @Test
    public void testCopySucceeds() throws ExecutionException, InterruptedException
    {
        // Insert Data
        Location bookLocation = insertBookData(client);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeCloneFetch = client.execute(fetchMobyDickOp);
        System.out.println("Before Clone Fetched: " + beforeCloneFetch);
        Book fetchedBook = beforeCloneFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location cloneLocation = new Location(booksBucket, "moby_dick_cloned");
        CloneValue cloneBook = new CloneValue.Builder(bookLocation, cloneLocation).build();

        CloneValue.Response cloneResp = client.execute(cloneBook);
        System.out.println("Clone resp: " + cloneResp);

        // Verify original still exists
        FetchValue.Response afterCloneFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        System.out.println("After clone fetch src: " + afterCloneFetch);
        fetchedBook = afterCloneFetch.getValue(Book.class);
        assertNotNull(fetchedBook);

        // Verify data was cloned
        FetchValue fetchClone = new FetchValue.Builder(cloneLocation).build();
        FetchValue.Response cloneFetchRes = client.execute(fetchClone);
        System.out.println("After clone fetch dest: " + cloneFetchRes);
        assertNotNull(cloneFetchRes.getValue(RiakObject.class));

    }

    private Location insertBookData(RiakClient client)
            throws ExecutionException, InterruptedException
    {
        Location bookLocation = new Location(booksBucket, "moby_dick");

        Book mobyDick = new Book();
        mobyDick.title = "Moby Dick";
        mobyDick.author = "Herman Melville";
        mobyDick.body = "Call me Ishmael. Some years ago...";
        mobyDick.isbn = "1111979723";
        mobyDick.copiesOwned = 3;

        StoreValue storeBookOp = new StoreValue.Builder(mobyDick).withLocation(bookLocation).build();
        client.execute(storeBookOp);

        return bookLocation;
    }

    public static class Book
    {
        @RiakVClock
        VClock vclock;
        @JsonProperty
        public String title;
        @JsonProperty
        public String author;
        @JsonProperty
        public String body;
        @JsonProperty
        public String isbn;
        @JsonProperty
        public Integer copiesOwned;
    }
}

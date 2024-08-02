package com.basho.riak.client.api.commands.itest;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.annotations.RiakVClock;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.commands.kv.CopyValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.operations.itest.ITestBase;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.*;

public class ITestCopy extends ITestBase {
    private final RiakClient client = new RiakClient(cluster);
    private final Namespace booksBucket = new Namespace("books");

    @Test
    public void testCopySucceeds() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = insertBookData(client);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeCloneFetch = client.execute(fetchMobyDickOp);
        System.out.println("Before Copy Fetched: " + beforeCloneFetch);
        Book fetchedBook = beforeCloneFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location copyLocation = new Location(new Namespace("copy-books"), "moby_dick");
        CopyValue copyBook = new CopyValue.Builder(bookLocation, copyLocation).build();

        CopyValue.Response copyResp = client.execute(copyBook);
        System.out.println("Copy resp: " + copyResp);

        // Verify original still exists
        FetchValue.Response afterCopyFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        System.out.println("After copy fetch src: " + afterCopyFetch);
        fetchedBook = afterCopyFetch.getValue(Book.class);
        assertNotNull(fetchedBook);

        // Verify data was cloned
        FetchValue fetchClone = new FetchValue.Builder(copyLocation).build();
        FetchValue.Response cloneFetchRes = client.execute(fetchClone);
        System.out.println("After clone fetch dest: " + cloneFetchRes);
        assertNotNull(cloneFetchRes.getValue(RiakObject.class));

        fail("TODO complete testing");
    }

    private Location insertBookData(RiakClient client)
            throws ExecutionException, InterruptedException {
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

    public static class Book {
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

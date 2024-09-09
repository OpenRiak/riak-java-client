package com.basho.riak.client.api.commands.itest;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.annotations.RiakVClock;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.commands.kv.CopyValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.netty.RiakResponseException;
import com.basho.riak.client.core.operations.itest.ITestBase;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.fail;

public class ITestCopy extends ITestBase {

    private final RiakClient client = new RiakClient(cluster);
    private final Namespace booksBucket = new Namespace("books");

    @Test
    public void testCopySucceeds() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick");
        insertBookData(client, bookLocation);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeCloneFetch = client.execute(fetchMobyDickOp);
        Book fetchedBook = beforeCloneFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location copyLocation = new Location(new Namespace("copy-books"), "moby_dick");
        CopyValue copyBook = new CopyValue.Builder(bookLocation, copyLocation).build();

        client.execute(copyBook);

        // Verify original still exists
        FetchValue.Response afterCopyFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        fetchedBook = afterCopyFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        // Verify data was cloned
        FetchValue fetchClone = new FetchValue.Builder(copyLocation).build();
        FetchValue.Response cloneFetchRes = client.execute(fetchClone);
        fetchedBook = cloneFetchRes.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");
    }

    @Test
    public void testCopySameBucket() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick_2");
        insertBookData(client, bookLocation);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeCloneFetch = client.execute(fetchMobyDickOp);
        Book fetchedBook = beforeCloneFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location copyLocation = new Location(booksBucket, "moby_dick_copy");
        CopyValue copyBook = new CopyValue.Builder(bookLocation, copyLocation).build();

        client.execute(copyBook);

        // Verify original still exists
        FetchValue.Response afterCopyFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        fetchedBook = afterCopyFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        // Verify data was cloned
        FetchValue fetchClone = new FetchValue.Builder(copyLocation).build();
        FetchValue.Response cloneFetchRes = client.execute(fetchClone);
        fetchedBook = cloneFetchRes.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");
    }

    @Test
    public void testCopyNoSourceLocation() throws ExecutionException, InterruptedException {
        Location sourceLocation = new Location(booksBucket, "unknown_source_location");
        Location copyLocation = new Location(new Namespace("copy-books"), "unused_dest_location");

        CopyValue copy = new CopyValue.Builder(sourceLocation, copyLocation).build();

        try {
            client.execute(copy);
            fail("Expected to fail");
        } catch (ExecutionException e) {
            assertEquals(e.getCause(), new RiakResponseException(0, "notfound"));
        }
    }

    @Test
    public void testCopyReturnBodyTrue() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick_3");
        insertBookData(client, bookLocation);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeCloneFetch = client.execute(fetchMobyDickOp);
        Book fetchedBook = beforeCloneFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location copyLocation = new Location(new Namespace("copy-books"), "moby_dick_2");
        CopyValue copyBook = new CopyValue.Builder(bookLocation, copyLocation)
                .withOption(CopyValue.Option.RETURN_BODY, true)
                .build();

        // Returned the body in the response
        CopyValue.Response response = client.execute(copyBook);
        fetchedBook = response.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        // Verify original still exists
        FetchValue.Response afterCopyFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        fetchedBook = afterCopyFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        // Verify data was cloned
        FetchValue fetchClone = new FetchValue.Builder(copyLocation).build();
        FetchValue.Response cloneFetchRes = client.execute(fetchClone);
        fetchedBook = cloneFetchRes.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");
    }

    @Test
    public void testMoveDestinationExists() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick_4");
        insertBookData(client, bookLocation);

        Location copyLocation = new Location(booksBucket, "moby_dick_5");
        insertBookData(client, bookLocation);

        CopyValue copy = new CopyValue.Builder(bookLocation, copyLocation).build();

        try {
            client.execute(copy);
            fail("Expected to fail");
        } catch (ExecutionException e) {
            assertEquals(e.getCause(), new RiakResponseException(0, "destination_not_empty"));
        }
    }

    private void insertBookData(RiakClient client, Location location)
            throws ExecutionException, InterruptedException {
        Book mobyDick = new Book();
        mobyDick.title = "Moby Dick";
        mobyDick.author = "Herman Melville";
        mobyDick.body = "Call me Ishmael. Some years ago...";
        mobyDick.isbn = "1111979723";
        mobyDick.copiesOwned = 3;

        StoreValue storeBookOp = new StoreValue.Builder(mobyDick).withLocation(location).build();
        client.execute(storeBookOp);
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

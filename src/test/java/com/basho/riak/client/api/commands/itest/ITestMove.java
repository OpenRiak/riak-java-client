package com.basho.riak.client.api.commands.itest;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.annotations.RiakVClock;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.MoveValue;
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
import static org.junit.Assert.*;

public class ITestMove extends ITestBase {

    private final RiakClient client = new RiakClient(cluster);
    private final Namespace booksBucket = new Namespace("books");

    @Test
    public void testMoveSucceeds() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick_m1");
        insertBookData(client, bookLocation);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeMoveFetch = client.execute(fetchMobyDickOp);
        Book fetchedBook = beforeMoveFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location moveLocation = new Location(new Namespace("move-books"), "moby_dick");
        MoveValue moveBook = new MoveValue.Builder(bookLocation, moveLocation).build();

        client.execute(moveBook);

        // Verify original is gone
        FetchValue.Response afterMoveFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        assertTrue(afterMoveFetch.isNotFound());
        fetchedBook = afterMoveFetch.getValue(Book.class);
        assertNull(fetchedBook);

        // Verify data was moved
        FetchValue fetchClone = new FetchValue.Builder(moveLocation).build();
        FetchValue.Response moveFetchRes = client.execute(fetchClone);
        fetchedBook = moveFetchRes.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");
    }

    @Test
    public void testMoveSameBucket() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick_m2");
        insertBookData(client, bookLocation);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeMoveFetch = client.execute(fetchMobyDickOp);
        Book fetchedBook = beforeMoveFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location moveLocation = new Location(booksBucket, "moby_dick_move");
        MoveValue moveBook = new MoveValue.Builder(bookLocation, moveLocation).build();

        client.execute(moveBook);

        // Verify original is gone
        FetchValue.Response afterMoveFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        assertTrue(afterMoveFetch.isNotFound());
        fetchedBook = afterMoveFetch.getValue(Book.class);
        assertNull(fetchedBook);

        // Verify data was moved
        FetchValue fetchClone = new FetchValue.Builder(moveLocation).build();
        FetchValue.Response moveFetchRes = client.execute(fetchClone);
        fetchedBook = moveFetchRes.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");
    }

    @Test
    public void testMoveNoSourceLocation() throws ExecutionException, InterruptedException {
        Location sourceLocation = new Location(booksBucket, "unknown_source_location");
        Location moveLocation = new Location(new Namespace("move-books"), "unused_dest_location");

        MoveValue move = new MoveValue.Builder(sourceLocation, moveLocation).build();

        try {
            client.execute(move);
            fail("Expected to fail");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof RiakResponseException);
            assertEquals(e.getCause().getMessage(), "notfound");
        }
    }

    @Test
    public void testMoveReturnBodyTrue() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick_m3");
        insertBookData(client, bookLocation);

        // Verify Data was inserted
        FetchValue fetchMobyDickOp = new FetchValue.Builder(bookLocation).build();
        FetchValue.Response beforeMoveFetch = client.execute(fetchMobyDickOp);
        Book fetchedBook = beforeMoveFetch.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        Location moveLocation = new Location(new Namespace("copy-books"), "moby_dick_move2");
        MoveValue moveBook = new MoveValue.Builder(bookLocation, moveLocation)
                .withOption(MoveValue.Option.RETURN_BODY, true)
                .build();

        // Returned the body in the response
        MoveValue.Response response = client.execute(moveBook);
        fetchedBook = response.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");

        // Verify original is gone
        FetchValue.Response afterMoveFetch = client.execute(new FetchValue.Builder(bookLocation).build());
        assertTrue(afterMoveFetch.isNotFound());
        fetchedBook = afterMoveFetch.getValue(Book.class);
        assertNull(fetchedBook);

        // Verify data was cloned
        FetchValue fetchClone = new FetchValue.Builder(moveLocation).build();
        FetchValue.Response moveFetchRes = client.execute(fetchClone);
        fetchedBook = moveFetchRes.getValue(Book.class);
        assertNotNull(fetchedBook);
        assertEquals(fetchedBook.author, "Herman Melville");
    }

    @Test
    public void testMoveDestinationExists() throws ExecutionException, InterruptedException {
        // Insert Data
        Location bookLocation = new Location(booksBucket, "moby_dick_m4");
        insertBookData(client, bookLocation);

        Location moveLocation = new Location(booksBucket, "moby_dick_m5");
        insertBookData(client, moveLocation);

        MoveValue move = new MoveValue.Builder(bookLocation, moveLocation).build();

        try {
            client.execute(move);
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

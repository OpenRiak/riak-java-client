package com.basho.riak.client.api.commands.kv;

import com.basho.riak.client.api.GenericRiakCommand;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.commands.RiakOption;
import com.basho.riak.client.core.FutureOperation;
import com.basho.riak.client.core.netty.RiakResponseException;
import com.basho.riak.client.core.operations.CloneOperation;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.util.BinaryValue;

import java.util.HashMap;
import java.util.Map;

public class MoveValue extends GenericRiakCommand.GenericRiakCommandWithSameInfo<MoveValue.Response, Location, CloneOperation.Response> {

    private final Map<MoveValue.Option<?>, Object> options = new HashMap<>();

    private final Location srcLocation;
    private final Location dstLocation;

    MoveValue(MoveValue.Builder builder) {
        this.options.putAll(builder.options);
        this.srcLocation = builder.srcLocation;
        this.dstLocation = builder.dstLocation;
    }


    @Override
    protected FutureOperation<CloneOperation.Response, ?, Location> buildCoreOperation() {
        CloneOperation.Builder builder = new CloneOperation.Builder(srcLocation, dstLocation);

        // Move deletes the source item so set it to true!
        builder.withDeleteSrc(true);

        for (Map.Entry<MoveValue.Option<?>, Object> opPair : options.entrySet()) {
            RiakOption<?> option = opPair.getKey();

            if (option == MoveValue.Option.SrcVClock) {
                builder.withSrcVClock((VClock) opPair.getValue());
            } else if (option == Option.DST_PROVENANCE_METADATA) {
                builder.withDstProvMeta((CloneOperation.Builder.ProvenanceMetadata) opPair.getValue());
            } else if (option == Option.RETURN_BODY) {
                builder.withReturnBody((Boolean) opPair.getValue());
            } else if (option == Option.R) {
                builder.withR(((Quorum) opPair.getValue()).getIntValue());
            } else if (option == Option.PR) {
                builder.withPR(((Quorum) opPair.getValue()).getIntValue());
            } else if (option == Option.W) {
                builder.withW(((Quorum) opPair.getValue()).getIntValue());
            } else if (option == Option.PW) {
                builder.withPW(((Quorum) opPair.getValue()).getIntValue());
            } else if (option == Option.DW) {
                builder.withDW(((Quorum) opPair.getValue()).getIntValue());
            } else if (option == Option.RW) {
                builder.withRW(((Quorum) opPair.getValue()).getIntValue());
            } else if (option == Option.N_VAL) {
                builder.withNVal((int) opPair.getValue());
            } else if (option == Option.TIMEOUT) {
                builder.withTimeout((int) opPair.getValue());
            } else if (option == Option.RECV_TIMEOUT) {
                builder.withRecvTimeout((int) opPair.getValue());
            } else if (option == Option.BASIC_QUORUM) {
                builder.withBasicQuorum((boolean) opPair.getValue());
            } else if (option == Option.SLOPPY_QUORUM) {
                builder.withSloppyQuorum((boolean) opPair.getValue());
            } else if (option == Option.NOTFOUND_OK) {
                builder.withNotFoundOk((boolean) opPair.getValue());
            } else if (option == Option.ASIS) {
                builder.withAsis((boolean) opPair.getValue());
            } else if (option == Option.DETAILS) {
                builder.withDetails((CloneOperation.Builder.Details) opPair.getValue());
            }
        }

        return builder.build();
    }

    @Override
    protected MoveValue.Response convertResponse(FutureOperation<CloneOperation.Response, ?, Location> request, CloneOperation.Response coreResponse) {
        Location loc = request.getQueryInfo();
        if (coreResponse.hasGeneratedKey()) {
            loc = new Location(loc.getNamespace(), coreResponse.getGeneratedKey());
        }

        // TODO do we want to expose any returned details?

        MoveValue.Response.Builder builder = new MoveValue.Response.Builder()
                .withValues(coreResponse.getObjectList())
                .withGeneratedKey(loc.getKey())
                .withLocation(loc);

        // We set delete source for moves so should check if it succeeded
        // leaves it to the caller to decide if this should be a failure
        // aka returns that the delete step may have failed or not completed yet but leaves the decision to fail on that
        // up to the caller
        if (coreResponse.hasDelFail()) {
            builder.withDelFail(coreResponse.getDelFail());
        }

        return builder.build();
    }

    public static class Response extends KvResponseBase {

        private final BinaryValue generatedKey;
        private final RiakResponseException delFail;

        Response(MoveValue.Response.Init<?> builder) {
            super(builder);
            this.generatedKey = builder.generatedKey;
            this.delFail = builder.delFail;
        }

        public boolean hasGeneratedKey() {
            return generatedKey != null;
        }

        public BinaryValue getGeneratedKey() {
            return generatedKey;
        }

        /**
         * Indicates whether delFail was returned, indicating that the delete source step of the move may have failed or not
         * be completed yet
         *
         * @return True if delFail set, false otherwise
         */
        public Boolean hasDelFail() {
            return delFail != null;
        }

        /**
         * Returns the set delFail from the clone operation,  indicating that the delete source step of the move may have failed or not
         * be completed yet
         *
         * @return The RiakResponseException for the delFail or null if not set
         */
        public RiakResponseException getDelFail() {
            return delFail;
        }

        protected static abstract class Init<T extends MoveValue.Response.Init<T>> extends KvResponseBase.Init<T> {

            private BinaryValue generatedKey;
            private RiakResponseException delFail;

            T withGeneratedKey(BinaryValue key) {
                this.generatedKey = key;
                return self();
            }

            T withDelFail(RiakResponseException delFail) {
                this.delFail = delFail;
                return self();
            }
        }

        static class Builder extends MoveValue.Response.Init<MoveValue.Response.Builder> {
            @Override
            protected MoveValue.Response.Builder self() {
                return this;
            }

            @Override
            MoveValue.Response build() {
                return new MoveValue.Response(this);
            }
        }
    }


    public final static class Option<T> extends RiakOption<T> {

        public static final MoveValue.Option<VClock> SrcVClock = new MoveValue.Option<>("SRC_VCLOCK");

        public static final MoveValue.Option<CloneOperation.Builder.ProvenanceMetadata> DST_PROVENANCE_METADATA = new MoveValue.Option<>("DST_PROVENANCE_METADATA");

        /**
         * Return Body.
         * Return the object stored in Riak. Note this will return all siblings.
         */
        public static final MoveValue.Option<Boolean> RETURN_BODY = new MoveValue.Option<>("RETURN_BODY");

        /**
         * Read Quorum.
         * How many replicas need to agree when fetching the object.
         */
        public static final MoveValue.Option<Quorum> R = new MoveValue.Option<>("R");
        /**
         * Primary Read Quorum.
         * How many primary replicas need to be available when retrieving the object.
         */
        public static final MoveValue.Option<Quorum> PR = new MoveValue.Option<>("PR");
        /**
         * Write Quorum.
         * How many replicas to write to before returning a successful response.
         */
        public static final MoveValue.Option<Quorum> W = new MoveValue.Option<>("W");
        /**
         * Primary Write Quorum.
         * How many primary nodes must be up when the write is attempted.
         */
        public static final MoveValue.Option<Quorum> PW = new MoveValue.Option<>("PW");
        /**
         * Durable Write Quorum.
         * How many replicas to commit to durable storage before returning a successful response.
         */
        public static final MoveValue.Option<Quorum> DW = new MoveValue.Option<>("DW");
        /**
         * Read Write Quorum.
         * Quorum for both operations (get and put) involved in deleting an object
         */
        public static final MoveValue.Option<Quorum> RW = new MoveValue.Option<>("RW");

        public static final MoveValue.Option<Integer> N_VAL = new MoveValue.Option<>("N_VAL");

        /**
         * Timeout.
         * Sets the server-side timeout for this operation. The default in Riak is 60 seconds.
         */
        public static final MoveValue.Option<Integer> TIMEOUT = new MoveValue.Option<>("TIMEOUT");
        public static final MoveValue.Option<Integer> RECV_TIMEOUT = new MoveValue.Option<>("RECV_TIMEOUT");

        /**
         * Basic Quorum.
         * Whether to return early in some failure cases (eg. when r=1 and you get
         * 2 errors and a success basic_quorum=true would return an error)
         */
        public static final MoveValue.Option<Boolean> BASIC_QUORUM = new MoveValue.Option<>("BASIC_QUORUM");
        public static final MoveValue.Option<Boolean> SLOPPY_QUORUM = new MoveValue.Option<>("SLOPPY_QUORUM");

        /**
         * Not Found OK.
         * Whether to treat notfounds as successful reads for the purposes of R
         */
        public static final MoveValue.Option<Boolean> NOTFOUND_OK = new MoveValue.Option<>("NOTFOUND_OK");

        public static final MoveValue.Option<Boolean> ASIS = new MoveValue.Option<>("ASIS");

        //public static final MoveValue.Option<String> SYNC_ON_WRITE = new MoveValue.Option<String>("SYNC_ON_WRITE");
        public static final MoveValue.Option<CloneOperation.Builder.Details> DETAILS = new MoveValue.Option<>("DETAILS");

        private Option(String name) {
            super(name);
        }
    }

    /**
     * Used to construct a MoveValue command.
     */
    public static class Builder {
        private final Map<MoveValue.Option<?>, Object> options = new HashMap<>();
        private final Location srcLocation;
        private final Location dstLocation;

        /**
         * Construct a Builder for a MoveValue command.
         *
         * @param src
         * @param dst
         */
        public Builder(Location src, Location dst) {
            this.srcLocation = src;
            this.dstLocation = dst;
        }

        /**
         * Add an optional setting for this command.
         * This will be passed along with the request to Riak to tell it how
         * to behave when servicing the request.
         *
         * @param option the option
         * @param value  the value for the option
         * @return a reference to this object.
         */
        public <T> MoveValue.Builder withOption(MoveValue.Option<T> option, T value) {
            options.put(option, value);
            return this;
        }

        /**
         * Construct the MoveValue command.
         *
         * @return the new MoveValue command.
         */
        public MoveValue build() {
            return new MoveValue(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (srcLocation != null ? srcLocation.hashCode() : 0);
        result = prime * result + (dstLocation != null ? dstLocation.hashCode() : 0);
        result = prime * result + options.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MoveValue)) {
            return false;
        }

        final MoveValue other = (MoveValue) obj;
        if (this.srcLocation != other.srcLocation && (this.srcLocation == null || !this.srcLocation.equals(other.srcLocation))) {
            return false;
        }
        if (this.dstLocation != other.dstLocation && (this.dstLocation == null || !this.dstLocation.equals(other.dstLocation))) {
            return false;
        }
        if (this.options != other.options && (this.options == null || !this.options.equals(other.options))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("{namespace: %s, key: %s, options: %s}",
                srcLocation, dstLocation, options);
    }

}

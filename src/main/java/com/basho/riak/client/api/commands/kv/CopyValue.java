package com.basho.riak.client.api.commands.kv;

import com.basho.riak.client.api.GenericRiakCommand;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.commands.RiakOption;
import com.basho.riak.client.core.FutureOperation;
import com.basho.riak.client.core.operations.CloneOperation;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.util.BinaryValue;

import java.util.HashMap;
import java.util.Map;

public class CopyValue extends GenericRiakCommand.GenericRiakCommandWithSameInfo<CopyValue.Response, Location, CloneOperation.Response> {

    private final Map<CopyValue.Option<?>, Object> options = new HashMap<>();

    private final Location srcLocation;
    private final Location dstLocation;

    CopyValue(CopyValue.Builder builder) {
        this.options.putAll(builder.options);
        this.srcLocation = builder.srcLocation;
        this.dstLocation = builder.dstLocation;
    }


    @Override
    protected FutureOperation<CloneOperation.Response, ?, Location> buildCoreOperation() {
        CloneOperation.Builder builder = new CloneOperation.Builder(srcLocation, dstLocation);

        // Copy does not delete the source item so set it to false
        builder.withDeleteSrc(false);

        for (Map.Entry<CopyValue.Option<?>, Object> opPair : options.entrySet()) {
            RiakOption<?> option = opPair.getKey();

            if (option == CopyValue.Option.SrcVClock) {
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
    protected CopyValue.Response convertResponse(FutureOperation<CloneOperation.Response, ?, Location> request, CloneOperation.Response coreResponse) {
        Location loc = request.getQueryInfo();
        if (coreResponse.hasGeneratedKey()) {
            loc = new Location(loc.getNamespace(), coreResponse.getGeneratedKey());
        }

        // TODO do we want to expose any returned details?

        // Note doesn't need to care about del_fail in response, should not have been asked to delete source
        // See buildCoreOperation clone requests sets delete to be false
        return new CopyValue.Response.Builder().withValues(coreResponse.getObjectList()).withGeneratedKey(loc.getKey()).withLocation(loc).build();
    }

    public static class Response extends KvResponseBase {

        private final BinaryValue generatedKey;

        Response(CopyValue.Response.Init<?> builder) {
            super(builder);
            this.generatedKey = builder.generatedKey;
        }

        public boolean hasGeneratedKey() {
            return generatedKey != null;
        }

        public BinaryValue getGeneratedKey() {
            return generatedKey;
        }

        protected static abstract class Init<T extends CopyValue.Response.Init<T>> extends KvResponseBase.Init<T> {

            private BinaryValue generatedKey;

            T withGeneratedKey(BinaryValue key) {
                this.generatedKey = key;
                return self();
            }
        }

        static class Builder extends CopyValue.Response.Init<CopyValue.Response.Builder> {
            @Override
            protected CopyValue.Response.Builder self() {
                return this;
            }

            @Override
            CopyValue.Response build() {
                return new CopyValue.Response(this);
            }
        }
    }


    public final static class Option<T> extends RiakOption<T> {

        public static final CopyValue.Option<VClock> SrcVClock = new CopyValue.Option<>("SRC_VCLOCK");

        public static final CopyValue.Option<CloneOperation.Builder.ProvenanceMetadata> DST_PROVENANCE_METADATA = new CopyValue.Option<>("DST_PROVENANCE_METADATA");

        /**
         * Return Body.
         * Return the object stored in Riak. Note this will return all siblings.
         */
        public static final CopyValue.Option<Boolean> RETURN_BODY = new CopyValue.Option<>("RETURN_BODY");

        /**
         * Read Quorum.
         * How many replicas need to agree when fetching the object.
         */
        public static final CopyValue.Option<Quorum> R = new CopyValue.Option<>("R");
        /**
         * Primary Read Quorum.
         * How many primary replicas need to be available when retrieving the object.
         */
        public static final CopyValue.Option<Quorum> PR = new CopyValue.Option<>("PR");
        /**
         * Write Quorum.
         * How many replicas to write to before returning a successful response.
         */
        public static final CopyValue.Option<Quorum> W = new CopyValue.Option<>("W");
        /**
         * Primary Write Quorum.
         * How many primary nodes must be up when the write is attempted.
         */
        public static final CopyValue.Option<Quorum> PW = new CopyValue.Option<>("PW");
        /**
         * Durable Write Quorum.
         * How many replicas to commit to durable storage before returning a successful response.
         */
        public static final CopyValue.Option<Quorum> DW = new CopyValue.Option<>("DW");
        /**
         * Read Write Quorum.
         * Quorum for both operations (get and put) involved in deleting an object
         */
        public static final CopyValue.Option<Quorum> RW = new CopyValue.Option<>("RW");

        public static final CopyValue.Option<Integer> N_VAL = new CopyValue.Option<>("N_VAL");

        /**
         * Timeout.
         * Sets the server-side timeout for this operation. The default in Riak is 60 seconds.
         */
        public static final CopyValue.Option<Integer> TIMEOUT = new CopyValue.Option<>("TIMEOUT");
        public static final CopyValue.Option<Integer> RECV_TIMEOUT = new CopyValue.Option<>("RECV_TIMEOUT");

        /**
         * Basic Quorum.
         * Whether to return early in some failure cases (eg. when r=1 and you get
         * 2 errors and a success basic_quorum=true would return an error)
         */
        public static final CopyValue.Option<Boolean> BASIC_QUORUM = new CopyValue.Option<>("BASIC_QUORUM");
        public static final CopyValue.Option<Boolean> SLOPPY_QUORUM = new CopyValue.Option<>("SLOPPY_QUORUM");

        /**
         * Not Found OK.
         * Whether to treat notfounds as successful reads for the purposes of R
         */
        public static final CopyValue.Option<Boolean> NOTFOUND_OK = new CopyValue.Option<>("NOTFOUND_OK");

        public static final CopyValue.Option<Boolean> ASIS = new CopyValue.Option<>("ASIS");

        //public static final CopyValue.Option<String> SYNC_ON_WRITE = new CopyValue.Option<String>("SYNC_ON_WRITE");
        public static final CopyValue.Option<CloneOperation.Builder.Details> DETAILS = new CopyValue.Option<>("DETAILS");

        private Option(String name) {
            super(name);
        }
    }

    /**
     * Used to construct a CopyValue command.
     */
    public static class Builder {
        private final Map<CopyValue.Option<?>, Object> options = new HashMap<>();
        private final Location srcLocation;
        private final Location dstLocation;

        /**
         * Construct a Builder for a CloneValue command.
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
        public <T> CopyValue.Builder withOption(CopyValue.Option<T> option, T value) {
            options.put(option, value);
            return this;
        }

        /**
         * Construct the CopyValue command.
         *
         * @return the new CopyValue command.
         */
        public CopyValue build() {
            return new CopyValue(this);
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
        if (!(obj instanceof CopyValue)) {
            return false;
        }

        final CopyValue other = (CopyValue) obj;
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
        return String.format("{namespace: %s, key: %s, options: %s}", srcLocation, dstLocation, options);
    }

}

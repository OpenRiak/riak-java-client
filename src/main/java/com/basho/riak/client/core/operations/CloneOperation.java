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

import com.basho.riak.client.api.cap.BasicVClock;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.core.FutureOperation;
import com.basho.riak.client.core.RiakMessage;
import com.basho.riak.client.core.converters.RiakObjectConverter;
import com.basho.riak.client.core.netty.RiakResponseException;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.util.BinaryValue;
import com.basho.riak.protobuf.RiakKvPB;
import com.basho.riak.protobuf.RiakMessageCodes;
import com.basho.riak.protobuf.RiakPB;
import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpExternal;
import com.ericsson.otp.erlang.OtpInputStream;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An operation used to clone an object in Riak.
 */
public class CloneOperation extends FutureOperation<CloneOperation.Response, RiakKvPB.RpbCloneResp, Location> {

    private final RiakKvPB.RpbCloneReq.Builder reqBuilder;
    private final Location srcLocation;

    private final Logger logger = LoggerFactory.getLogger(CloneOperation.class);

    private CloneOperation(Builder builder) {
        this.reqBuilder = builder.reqBuilder;
        this.srcLocation = builder.srcLocation;
    }

    @Override
    protected RiakKvPB.RpbCloneResp decode(RiakMessage message) {
        Operations.checkPBMessageType(message, RiakMessageCodes.MSG_CloneResp);

        try {
            byte[] data = message.getData();
            // Expecting the data to not be empty otherwise invalid, if the source wasn't found it would result in an
            // error message being returned instead (aka "notfound")
            return RiakKvPB.RpbCloneResp.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Invalid message received", e);
            throw new IllegalArgumentException("Invalid message received", e);
        }
    }

    @Override
    protected CloneOperation.Response convert(List<RiakKvPB.RpbCloneResp> responses) {
        // This is not a streaming op, there will only be one response
        if (responses.size() > 1) {
            logger.error("Received {} responses when only one was expected.", responses.size());
        }

        final RiakKvPB.RpbCloneResp response = responses.get(0);

        return convert(response);
    }

    private CloneOperation.Response convert(RiakKvPB.RpbCloneResp response) {
        CloneOperation.Response.Builder responseBuilder = new CloneOperation.Response.Builder();

        // This only exists if no key was specified in the put request
        if (response.hasKey()) {
            responseBuilder.withGeneratedKey(BinaryValue.unsafeCreate(response.getKey().toByteArray()));
        }

        // Only exists if the request has delete_src=true
        if (response.hasDelFail()) {
            try {
                OtpInputStream is = new OtpInputStream(response.getDelFail().toByteArray());

                //When present, the del_error fields will be an ETF-encoded term. Likely values:
                // - atom
                // - tuple(atom, integer)
                // - tuple(atom, integer, integer)

                int firstByte = is.read1skip_version();
                is.reset();

                if (firstByte == OtpExternal.smallTupleTag || firstByte == OtpExternal.largeTupleTag) {
                    int arity = is.read_tuple_head();
                    String atom = is.read_atom();
                    int code = 0;
                    if (arity > 1) {
                        code = is.read_int();
                    }
                    responseBuilder.withDelFail(new RiakResponseException(code, atom));
                } else if (firstByte == OtpExternal.atomTag) {
                    String atom = is.read_atom();
                    responseBuilder.withDelFail(new RiakResponseException(0, atom));
                } else {
                    // Don't know try and decode it and use it
                    String msg = OtpErlangObject.decode(is).toString();
                    responseBuilder.withDelFail(new RiakResponseException(0, msg));
                }
            } catch (OtpErlangDecodeException e) {
                logger.error("DelFail was present in RpbCloneResp but could not parse it", e);
                responseBuilder.withDelFail(new RiakResponseException(0, "DelFail was present in RpbCloneResp but could not parse it"));
            }
        }

        // Only exists if the request had details requested
        List<RiakPB.RpbPair> details = response.getDetailsList();
        if (!details.isEmpty()) {
            // details, when requested, is a list of {key = atom, value = ETF-encoded} pairs, where values are likely:
            // - integer (microseconds)
            // - list(tuple(atom, integer))
            // - maybe (not sure) (b) values could be a deep (nested) list of (b)
            // For now opting to return the keys and values as strings, unsure how we want to expose this yet
            Map<String, String> parsed = details.stream().collect(Collectors.toMap(
                    pair -> pair.getKey().toStringUtf8(),
                    pair -> pair.getValue().toStringUtf8()
            ));
            responseBuilder.withDetails(parsed);
        }

        // Note RiakMessageCodec and RiakMessage has a handling for `response.getError()`
        // its treated as an error and listeners should already be invoked, aka should not reach here if had the error
        // field set

        // To unify the behavior of having just a tombstone vs. siblings
        // that include a tombstone, we create an empty object and mark
        // it deleted
        if (response.getContentCount() == 0) {
            RiakObject ro = new RiakObject().setDeleted(true).setVClock(new BasicVClock(response.getVclock().toByteArray()));

            responseBuilder.addObject(ro);
        } else {
            responseBuilder.addObjects(RiakObjectConverter.convert(response.getContentList(), response.getVclock()));
        }

        return responseBuilder.build();
    }

    @Override
    protected RiakMessage createChannelMessage() {
        RiakKvPB.RpbCloneReq req = reqBuilder.build();
        return new RiakMessage(RiakMessageCodes.MSG_CloneReq, req.toByteArray());
    }

    @Override
    public Location getQueryInfo() {
        return srcLocation;
    }

    public static class Builder {
        private final RiakKvPB.RpbCloneReq.Builder reqBuilder = RiakKvPB.RpbCloneReq.newBuilder();
        private final Location srcLocation;
        private final Location dstLocation;

        /**
         * Construct a CloneOperation that will retrieve an object from Riak stored
         * at the provided Location.
         *
         * @param srcLocation the location of the object to clone
         * @param dstLocation the location of where to clone the object
         */
        public Builder(Location srcLocation, Location dstLocation) {
            if (srcLocation == null) {
                throw new IllegalArgumentException("srcLocation can not be null.");
            }

            if (dstLocation == null) {
                throw new IllegalArgumentException("dstLocation can not be null.");
            }

            reqBuilder.setSrcKey(ByteString.copyFrom(srcLocation.getKey().unsafeGetValue()));
            reqBuilder.setSrcBucket(ByteString.copyFrom(srcLocation.getNamespace().getBucketName().unsafeGetValue()));
            reqBuilder.setSrcBucketType(ByteString.copyFrom(srcLocation.getNamespace().getBucketType().unsafeGetValue()));

            reqBuilder.setDstKey(ByteString.copyFrom(dstLocation.getKey().unsafeGetValue()));
            reqBuilder.setDstBucket(ByteString.copyFrom(dstLocation.getNamespace().getBucketName().unsafeGetValue()));
            reqBuilder.setDstBucketType(ByteString.copyFrom(dstLocation.getNamespace().getBucketType().unsafeGetValue()));
            reqBuilder.setDeleteSrc(false);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        /**
         * Set whether to delete the src on clone.
         *
         * @param deleteSrc boolean
         * @return a reference to this object.
         */
        public Builder withDeleteSrc(boolean deleteSrc) {
            reqBuilder.setDeleteSrc(deleteSrc);
            return this;
        }


        public Builder withSrcVClock(VClock vClock) {
            reqBuilder.setSrcVclock(ByteString.copyFrom(vClock.getBytes()));
            return this;
        }

        public Builder withDstProvMeta(ProvenanceMetadata provMeta) {
            reqBuilder.setDstProvMeta(ByteString.copyFromUtf8(provMeta.toString()));
            return this;
        }

        public Builder withReturnBody(boolean returnBody) {
            reqBuilder.setReturnBody(returnBody);
            return this;
        }

        public Builder withR(int r) {
            reqBuilder.setR(r);
            return this;
        }

        public Builder withPR(int pr) {
            reqBuilder.setPr(pr);
            return this;
        }

        public Builder withW(int w) {
            reqBuilder.setW(w);
            return this;
        }

        public Builder withPW(int pw) {
            reqBuilder.setPw(pw);
            return this;
        }

        public Builder withDW(int dw) {
            reqBuilder.setDw(dw);
            return this;
        }

        public Builder withRW(int rw) {
            reqBuilder.setRw(rw);
            return this;
        }

        public Builder withNVal(int nval) {
            reqBuilder.setNVal(nval);
            return this;
        }

        public Builder withTimeout(int timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("Timeout can not be zero or less");
            }
            reqBuilder.setTimeout(timeout);
            return this;
        }

        public Builder withRecvTimeout(int recvTimeout) {
            if (recvTimeout <= 0) {
                throw new IllegalArgumentException("Timeout can not be zero or less");
            }
            reqBuilder.setRecvTimeout(recvTimeout);
            return this;
        }

        public Builder withBasicQuorum(boolean basicQuorum) {
            reqBuilder.setBasicQuorum(basicQuorum);
            return this;
        }

        public Builder withSloppyQuorum(boolean sloppyQuorum) {
            reqBuilder.setSloppyQuorum(sloppyQuorum);
            return this;
        }

        public Builder withNotFoundOk(boolean notFoundOk) {
            reqBuilder.setNotfoundOk(notFoundOk);
            return this;
        }

        public Builder withAsis(boolean asis) {
            reqBuilder.setAsis(asis);
            return this;
        }

//        public Builder withSyncOnWrite(String asis) {
//            reqBuilder.setSyncOnWrite(ByteString.copyFromUtf8(asis));
//            return this;
//        }

        public Builder withDetails(Details details) {
            reqBuilder.addDetails(ByteString.copyFromUtf8(details.toString()));
            return this;
        }


        public Builder self() {
            return this;
        }

        public CloneOperation build() {
            return new CloneOperation(this);
        }


        public enum Details {
            TIMING("timing"), VNODES("vnodes"), TRUE("true"), FALSE("false");

            final String detailsStr;

            Details(String detailsStr) {
                this.detailsStr = detailsStr;
            }
        }

        public enum ProvenanceMetadata {
            STORE("store"), STRIP("strip");

            final String provMetaStr;

            ProvenanceMetadata(String provMetaStr) {
                this.provMetaStr = provMetaStr;
            }

            public String toString() {
                return this.provMetaStr;
            }
        }
    }

    public static class Response extends FetchOperation.KvResponseBase {

        private final BinaryValue generatedKey;
        private final RiakResponseException delFail;
        private final Map<String, String> details;

        private Response(Init<?> builder) {
            super(builder);
            this.generatedKey = builder.generatedKey;
            this.delFail = builder.delFail;
            this.details = builder.details;
        }

        public boolean hasGeneratedKey() {
            return generatedKey != null;
        }

        public BinaryValue getGeneratedKey() {
            return generatedKey;
        }

        public boolean hasDelFail() {
            return delFail != null;
        }

        public RiakResponseException getDelFail() {
            return delFail;
        }

        public boolean hasDetails() {
            return details != null && !details.isEmpty();
        }

        public Map<String, String> getDetails() {
            return details;
        }

        protected static abstract class Init<T extends Init<T>> extends FetchOperation.KvResponseBase.Init<T> {
            private BinaryValue generatedKey;
            private RiakResponseException delFail;
            private Map<String, String> details;

            T withGeneratedKey(BinaryValue key) {
                this.generatedKey = key;
                return self();
            }

            T withDelFail(RiakResponseException delFail) {
                this.delFail = delFail;
                return self();
            }

            T withDetails(Map<String, String> details) {
                this.details = details;
                return self();
            }
        }

        static class Builder extends Init<Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            protected Response build() {
                return new Response(this);
            }
        }
    }

}

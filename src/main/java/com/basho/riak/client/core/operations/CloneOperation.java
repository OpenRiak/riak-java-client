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

import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.core.AllQuorumOptionsBuilder;
import com.basho.riak.client.core.FutureOperation;
import com.basho.riak.client.core.RiakMessage;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.protobuf.RiakKvPB;
import com.basho.riak.protobuf.RiakMessageCodes;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An operation used to clone an object in Riak.
 */
public class CloneOperation extends FutureOperation<CloneOperation.Response, RiakKvPB.RpbCloneResp, Location>
{
    private final RiakKvPB.RpbCloneReq.Builder reqBuilder;
    private final Location srcLocation;
    private final Location dstLocation;

    private final Logger logger = LoggerFactory.getLogger(CloneOperation.class);

    private CloneOperation(Builder builder)
    {
        this.reqBuilder = builder.reqBuilder;
        this.srcLocation = builder.srcLocation;
        this.dstLocation = builder.dstLocation;
    }

    @Override
    protected RiakKvPB.RpbCloneResp decode(RiakMessage message)
    {
        Operations.checkPBMessageType(message, RiakMessageCodes.MSG_CloneResp);
        return null;
    }

    @Override
    protected CloneOperation.Response convert(List<RiakKvPB.RpbCloneResp> responses)
    {
        // This is not a streaming op, there will only be one response
        if (responses.size() > 1)
        {
            logger.error("Received {} responses when only one was expected.", responses.size());
        }

        final RiakKvPB.RpbCloneResp response = responses.get(0);
        return convert(response);
    }

    static CloneOperation.Response convert(RiakKvPB.RpbCloneResp response)
    {
        CloneOperation.Response.Builder responseBuilder =
                new CloneOperation.Response.Builder();

        // If the response is null ... it means not found. Riak only sends
        // a message code and zero bytes when that's the case. (See: decode() )
        // Because that makes sense!
        if (null == response)
        {
            responseBuilder.withNotFound(true);
        }

        return responseBuilder.build();
    }

    @Override
    protected RiakMessage createChannelMessage()
    {
        RiakKvPB.RpbCloneReq req = reqBuilder.build();
        return new RiakMessage(RiakMessageCodes.MSG_CloneReq, req.toByteArray());
    }

    @Override
    public Location getQueryInfo()
    {
        return srcLocation;
    }

    public static class Builder extends AllQuorumOptionsBuilder<Builder>
    {
        private final RiakKvPB.RpbCloneReq.Builder reqBuilder =
            RiakKvPB.RpbCloneReq.newBuilder();
        private final Location srcLocation;
        private final Location dstLocation;

        /**
         * Construct a CloneOperation that will retrieve an object from Riak stored
         * at the provided Location.
         * @param srcLocation the location of the object to clone
         * @param dstLocation the location of where to clone the object
         */
        public Builder(Location srcLocation, Location dstLocation)
        {
            if (srcLocation == null)
            {
                throw new IllegalArgumentException("srcLocation can not be null.");
            }

            if (dstLocation == null)
            {
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
        public Builder withDeleteSrc(boolean deleteSrc)
        {
            reqBuilder.setDeleteSrc(deleteSrc);
            return this;
        }

        /**
         * Set the src vclock
         *
         * @param vclock
         * @return a reference to this object.
         */
        public Builder withSrcVClock(VClock vClock)
        {
            reqBuilder.setSrcVclock(ByteString.copyFrom(vClock.getBytes()));
            return this;
        }

        public Builder self()
        {
            return this;
        }

        public CloneOperation build()
        {
            reqBuilder.setGetQuorum(getOptionBuilder.build());
            reqBuilder.setPutQuorum(putOptionBuilder.build());
            reqBuilder.setDelQuorum(delOptionBuilder.build());
            return new CloneOperation(this);
        }

    }

    protected static abstract class KvResponseBase
    {
        private final List<RiakObject> objectList;

        protected KvResponseBase(Init<?> builder)
        {
            this.objectList = builder.objectList;
        }

        public List<RiakObject> getObjectList()
        {
            return objectList;
        }

        protected static abstract class Init<T extends Init<T>>
        {
            private final List<RiakObject> objectList = new LinkedList<>();
            protected abstract T self();
            protected abstract KvResponseBase build();

            T addObject(RiakObject object)
            {
                objectList.add(object);
                return self();
            }

            T addObjects(List<RiakObject> objects)
            {
                objectList.addAll(objects);
                return self();
            }
        }
    }

    public static class Response extends KvResponseBase
    {
        private final boolean notFound;

        private Response(Init<?> builder)
        {
            super(builder);
            this.notFound = builder.notFound;
        }

        public boolean isNotFound()
        {
            return notFound;
        }

        protected static abstract class Init<T extends Init<T>> extends KvResponseBase.Init<T>
        {
            private boolean notFound;

            T withNotFound(boolean notFound)
            {
                this.notFound = notFound;
                return self();
            }
        }

        static class Builder extends Init<Builder>
        {
            @Override
            protected Builder self()
            {
                return this;
            }

            @Override
            protected Response build()
            {
                return new Response(this);
            }
        }
    }
}

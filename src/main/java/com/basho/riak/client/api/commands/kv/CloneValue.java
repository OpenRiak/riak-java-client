/*
 * Copyright 2013 Basho Technologies Inc
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
package com.basho.riak.client.api.commands.kv;

import com.basho.riak.client.api.GenericRiakCommand;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.cap.VClock;
import com.basho.riak.client.api.convert.Converter;
import com.basho.riak.client.api.convert.Converter.OrmExtracted;
import com.basho.riak.client.api.convert.ConverterFactory;
import com.basho.riak.client.core.FutureOperation;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.operations.CloneOperation;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.api.commands.RiakOption;
import com.basho.riak.client.core.util.BinaryValue;
import com.basho.riak.protobuf.RiakKvPB;

import java.util.HashMap;
import java.util.Map;

import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.fasterxml.jackson.core.type.TypeReference;

public final class CloneValue extends GenericRiakCommand.GenericRiakCommandWithSameInfo<CloneValue.Response,
        Location, CloneOperation.Response>
{
    private final Map<Option<?>, Object> options = new HashMap<>();
    private final Location srcLocation;
    private final Location dstLocation;

    CloneValue(Builder builder)
    {
        this.options.putAll(builder.options);
        this.srcLocation = builder.srcLocation;
        this.dstLocation = builder.dstLocation;
    }


    @Override
    protected Response convertResponse(FutureOperation<CloneOperation.Response, ?, Location> request,
                                       CloneOperation.Response coreResponse)
    {
        return new Response.Builder()
                .build();
    }

    @Override
    protected RiakFuture<Response, Location> executeAsync(RiakCluster cluster)
    {
        return super.executeAsync(cluster);
    }

    @Override
    protected CloneOperation buildCoreOperation()
    {
        CloneOperation.Builder builder = new CloneOperation.Builder(srcLocation, dstLocation);

        for (Map.Entry<Option<?>, Object> opPair : options.entrySet())
        {
            RiakOption<?> option = opPair.getKey();

            /*
            if (option == Option.GetQuorumOptions)
            {
                builder.withGetQuorumOptions((RiakKvPB.RpbGetQuorumOpts) opPair.getValue());
            }
            else if (option == Option.PutQuorumOptions)
            {
                builder.withPutQuorumOptions((RiakKvPB.RpbPutQuorumOpts) opPair.getValue());
            }
            else if (option == Option.DelQuorumOptions)
            {
                builder.withDelQuorumOptions((RiakKvPB.RpbDelQuorumOpts) opPair.getValue());
            }
            */
            if (option == Option.DeleteSrc)
            {
                builder.withDeleteSrc((Boolean) opPair.getValue());
            }
            else if (option == Option.SrcVClock)
            {
                builder.withSrcVClock((VClock) opPair.getValue());
            }

        }

        return builder.build();
    }

    /**
    * Options For controlling how Riak performs the clone operation.
    * <p>
    * These options can be supplied to the {@link CloneValue.Builder} to change
    * how Riak performs the operation. These override the defaults provided
    * by the bucket.
    * </p>
    * @since 2.0
    * @see <a href="http://docs.basho.com/riak/latest/dev/advanced/cap-controls/">Replication Properties</a>
    */
   public final static class Option<T> extends RiakOption<T>
   {
       public static final Option<RiakKvPB.RpbGetQuorumOpts> GetQuorumOptions = new Option<>("GET_QUORUM_OPTIONS");
       public static final Option<RiakKvPB.RpbPutQuorumOpts> PutQuorumOptions = new Option<>("PUT_QUORUM_OPTIONS");
       public static final Option<RiakKvPB.RpbDelQuorumOpts> DelQuorumOptions = new Option<>("DEL_QUORUM_OPTIONS");

       public static final Option<Boolean> DeleteSrc = new Option<>("DELETE_SRC");
       public static final Option<VClock> SrcVClock = new Option<>("SRC_VCLOCK");

       private Option(String name)
       {
           super(name);
       }
   }

    public static class Response extends KvResponseBase
    {
        Response(Init<?> builder)
        {
            super(builder);
        }

        static class Builder extends Init<Builder>
        {
            @Override
            protected Builder self()
            {
                return this;
            }

            @Override
            Response build()
            {
                return new Response(this);
            }
        }
    }



    /**
     * Used to construct a CloneValue command.
     */
    public static class Builder
    {
        private final Map<Option<?>, Object> options = new HashMap<>();
        private final Location srcLocation;
        private final Location dstLocation;

        /**
         * Construct a Builder for a CloneValue command.
         * @param src
         * @param dst
         */
        public Builder(Location src, Location dst)
        {
            this.srcLocation = src;
            this.dstLocation = dst;
        }

        /**
         * Add an optional setting for this command.
         * This will be passed along with the request to Riak to tell it how
         * to behave when servicing the request.
         *
         * @param option the option
         * @param value the value for the option
         * @return a reference to this object.
         */
        public <T> Builder withOption(Option<T> option, T value)
        {
            options.put(option, value);
            return this;
        }

        /**
         * Construct the CloneValue command.
         * @return the new CloneValue command.
         */
        public CloneValue build()
        {
            return new CloneValue(this);
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (srcLocation != null ? srcLocation.hashCode() : 0);
        result = prime * result + (dstLocation != null ? dstLocation.hashCode() : 0);
        result = prime * result + options.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof CloneValue))
        {
            return false;
        }

        final CloneValue other = (CloneValue) obj;
        if (this.srcLocation != other.srcLocation && (this.srcLocation == null || !this.srcLocation.equals(other.srcLocation)))
        {
            return false;
        }
        if (this.dstLocation != other.dstLocation && (this.dstLocation == null || !this.dstLocation.equals(other.dstLocation)))
        {
            return false;
        }
        if (this.options != other.options && (this.options == null || !this.options.equals(other.options)))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("{namespace: %s, key: %s, options: %s}",
                srcLocation, dstLocation, options);
    }
}

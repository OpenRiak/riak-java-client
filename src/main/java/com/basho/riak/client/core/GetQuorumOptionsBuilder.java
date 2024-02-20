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

package com.basho.riak.client.core;

import com.basho.riak.protobuf.RiakKvPB;

public abstract class GetQuorumOptionsBuilder<T extends GetQuorumOptionsBuilder<T>>
{
    protected final RiakKvPB.RpbGetQuorumOpts.Builder getOptionBuilder
        = RiakKvPB.RpbGetQuorumOpts.newBuilder();

    protected abstract T self();

    /**
     * Set the r value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param r the r value as an integer.
     * @return a reference to this object.
     */
    public T withR(int r)
    {
        getOptionBuilder.setR(r);
        return self();
    }

    /**
     * Set the pr value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param pr the pr value as an integer.
     * @return a reference to this object.
     */
    public T withPr(int pr)
    {
        getOptionBuilder.setPr(pr);
        return self();
    }
    /**
     * Set the nVal.
     *
     * @param nVal the number of replicas.
     * @return a reference to this object.
     */
    public T withNVal(int nVal)
    {
        if (nVal <= 0)
        {
            throw new IllegalArgumentException("nVal must be >= 1");
        }
        getOptionBuilder.setNVal(nVal);
        return self();
    }
}

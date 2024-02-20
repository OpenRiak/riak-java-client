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

import com.basho.riak.protobuf.RiakKvPB;

public abstract class PutQuorumOptionsBuilder<T extends PutQuorumOptionsBuilder<T>>
{
    protected final RiakKvPB.RpbPutQuorumOpts.Builder putOptionBuilder
        = RiakKvPB.RpbPutQuorumOpts.newBuilder();

    protected abstract T self();

    /**
     * Set the dw value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param dw the dw value as an integer.
     * @return a reference to this object.
     */
    public T withDw(int dw)
    {
        putOptionBuilder.setDw(dw);
        return self();
    }

    /**
     * @param pw the pw value as an integer.
     * @return a reference to this object.
     */
    public T withPw(int pw)
    {
        putOptionBuilder.setPw(pw);
        return self();
    }

    /**
     * Set the w value. Individual requests (or buckets in a bucket type)
     * can override this.
     *
     * @param w the w value as an integer.
     * @return a reference to this object.
     */
    public T withW(int w)
    {
        putOptionBuilder.setW(w);
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
        putOptionBuilder.setNVal(nVal);
        return self();
    }
}
